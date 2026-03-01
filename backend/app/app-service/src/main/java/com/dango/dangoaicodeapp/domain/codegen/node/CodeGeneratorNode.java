package com.dango.dangoaicodeapp.domain.codegen.node;

import cn.hutool.core.util.StrUtil;
import com.dango.aicodegenerate.model.QualityResult;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.CodeGenerationStreamPort;
import com.dango.dangoaicodeapp.domain.codegen.port.ProjectScaffoldPort;
import com.dango.dangoaicodeapp.domain.codegen.port.ProjectWorkspacePort;
import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowMessagePort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码生成节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGeneratorNode {

    private static final String NODE_NAME = "代码生成";

    private final WorkflowMessagePort workflowMessagePort;
    private final CodeGenerationStreamPort codeGenerationStreamPort;
    // 节点只依赖“脚手架准备”能力，不直接感知模板复制/软链等实现细节。
    private final ProjectScaffoldPort projectScaffoldPort;
    private final ProjectWorkspacePort projectWorkspacePort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            workflowMessagePort.emitNodeStart(context.getWorkflowExecutionId(), NODE_NAME);

            String userMessage = buildUserMessage(context);
            CodeGenTypeEnum generationType = context.getGenerationType();
            if (generationType == null) {
                generationType = CodeGenTypeEnum.VUE_PROJECT;
            }
            Long appId = context.getAppId();

            boolean isFixMode = isQualityCheckFailed(context.getQualityResult());
            if (isFixMode) {
                workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, "检测到代码质量问题，正在修复...\n");
            } else {
                workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME,
                        String.format("开始生成 %s 类型代码...\n", generationType.getText()));
            }

            try {
                projectScaffoldPort.scaffold(appId, generationType);
                workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, "项目模板已就绪\n");

                Flux<String> codeStream = codeGenerationStreamPort.generateAndSaveCodeStream(
                        userMessage, generationType, appId);

                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Throwable> errorRef = new AtomicReference<>();

                codeStream
                        .subscribeOn(Schedulers.immediate())
                        .doOnNext(context::emit)
                        .doOnError(errorRef::set)
                        .doFinally(signalType -> latch.countDown())
                        .subscribe();

                latch.await();

                if (errorRef.get() != null) {
                    throw new RuntimeException(errorRef.get());
                }

                String generatedCodeDir = projectWorkspacePort.buildGeneratedCodeDir(generationType, appId);
                context.setGeneratedCodeDir(generatedCodeDir);
                context.setQualityResult(null);

                log.info("代码生成完成，目录: {}", generatedCodeDir);
                workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, "\n代码生成并保存完成\n");

            } catch (Exception e) {
                log.error("代码生成失败: {}", e.getMessage(), e);
                context.setErrorMessage("代码生成失败: " + e.getMessage());
                workflowMessagePort.emitNodeError(context.getWorkflowExecutionId(), NODE_NAME, e.getMessage());
            }

            workflowMessagePort.emitNodeComplete(context.getWorkflowExecutionId(), NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }

    private static String buildUserMessage(WorkflowContext context) {
        String userMessage = context.getEnhancedPrompt();

        QualityResult qualityResult = context.getQualityResult();
        if (isQualityCheckFailed(qualityResult)) {
            userMessage = buildErrorFixPrompt(context.getEnhancedPrompt(), qualityResult);
        }

        return userMessage;
    }

    private static boolean isQualityCheckFailed(QualityResult qualityResult) {
        return qualityResult != null &&
                !qualityResult.getIsValid() &&
                qualityResult.getErrors() != null &&
                !qualityResult.getErrors().isEmpty();
    }

    private static String buildErrorFixPrompt(String originalPrompt, QualityResult qualityResult) {
        StringBuilder errorInfo = new StringBuilder();

        if (StrUtil.isNotBlank(originalPrompt)) {
            errorInfo.append("## 原始需求：\n").append(originalPrompt).append("\n\n");
        }

        errorInfo.append("## 上次生成的代码存在以下问题，请修复：\n");

        qualityResult.getErrors().forEach(error ->
                errorInfo.append("- ").append(error).append("\n"));

        if (qualityResult.getSuggestions() != null && !qualityResult.getSuggestions().isEmpty()) {
            errorInfo.append("\n## 修复建议：\n");
            qualityResult.getSuggestions().forEach(suggestion ->
                    errorInfo.append("- ").append(suggestion).append("\n"));
        }

        return errorInfo.toString();
    }
}
