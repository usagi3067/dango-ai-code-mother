package com.dango.dangoaicodeapp.domain.codegen.node;

import cn.hutool.core.util.StrUtil;
import com.dango.aicodegenerate.model.QualityResult;
import com.dango.dangoaicodeapp.domain.codegen.service.AiCodeGeneratorFacade;
import com.dango.dangoaicodeapp.domain.codegen.scaffold.ProjectScaffoldServiceFactory;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码生成节点
 * 调用 AI 服务生成代码并保存，支持流式输出
 */
@Slf4j
@Component
public class CodeGeneratorNode {

    private static final String NODE_NAME = "代码生成";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            // 发送节点开始消息（使用 context 而非 ThreadLocal）
            context.emitNodeStart(NODE_NAME);

            // 构造用户消息（包含原始提示词和可能的错误修复信息）
            String userMessage = buildUserMessage(context);
            CodeGenTypeEnum generationType = context.getGenerationType();
            if (generationType == null) {
                generationType = CodeGenTypeEnum.VUE_PROJECT;
            }
            Long appId = context.getAppId();

            // 判断是否是修复模式
            boolean isFixMode = isQualityCheckFailed(context.getQualityResult());
            if (isFixMode) {
                context.emitNodeMessage(NODE_NAME, "检测到代码质量问题，正在修复...\n");
            } else {
                context.emitNodeMessage(NODE_NAME,
                        String.format("开始生成 %s 类型代码...\n", generationType.getText()));
            }

            try {
                // 恢复监控上下文到当前线程（用于跨线程传递监控信息）
                context.restoreMonitorContext();

                // 获取 AI 代码生成外观服务
                AiCodeGeneratorFacade codeGeneratorFacade = SpringContextUtil.getBean(AiCodeGeneratorFacade.class);

                // 复制模板脚手架到项目目录（创建模式时）
                ProjectScaffoldServiceFactory scaffoldFactory = SpringContextUtil.getBean(ProjectScaffoldServiceFactory.class);
                scaffoldFactory.getService(generationType).scaffold(appId);
                context.emitNodeMessage(NODE_NAME, "项目模板已就绪\n");

                // 使用流式生成，实时输出代码内容
                Flux<String> codeStream = codeGeneratorFacade.generateAndSaveCodeStream(
                        userMessage, generationType, appId);

                // 使用 CountDownLatch 等待流式生成完成
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<Throwable> errorRef = new AtomicReference<>();

                // 订阅流，实时输出到前端
                // 使用 subscribeOn(Schedulers.immediate()) 确保在当前线程执行
                codeStream
                        .subscribeOn(Schedulers.immediate())
                        .doOnNext(chunk -> {
                            // 实时输出代码片段到前端（使用 context 而非 ThreadLocal）
                            context.emit(chunk);
                        })
                        .doOnError(error -> {
                            errorRef.set(error);
                        })
                        .doFinally(signalType -> {
                            latch.countDown();
                        })
                        .subscribe();

                // 等待流式生成完成
                latch.await();

                // 检查是否有错误
                if (errorRef.get() != null) {
                    throw new RuntimeException(errorRef.get());
                }

                // 构建生成的代码目录路径
                String generatedCodeDir = buildGeneratedCodeDir(generationType, appId);
                context.setGeneratedCodeDir(generatedCodeDir);

                // 清除质量检查结果（重新生成后需要重新检查）
                context.setQualityResult(null);

                log.info("代码生成完成，目录: {}", generatedCodeDir);
                context.emitNodeMessage(NODE_NAME, "\n代码生成并保存完成\n");

            } catch (Exception e) {
                log.error("代码生成失败: {}", e.getMessage(), e);
                context.setErrorMessage("代码生成失败: " + e.getMessage());
                context.emitNodeError(NODE_NAME, e.getMessage());
            } finally {
                // 清除当前线程的监控上下文
                context.clearMonitorContext();
            }

            // 发送节点完成消息
            context.emitNodeComplete(NODE_NAME);

            // 更新状态
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 构建生成的代码目录路径
     */
    private static String buildGeneratedCodeDir(CodeGenTypeEnum generationType, Long appId) {
        // 与 CodeFileSaverExecutor 保持一致的目录命名规则
        String baseDir = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "code_output";
        String dirName = generationType.getValue() + "_" + appId;
        return baseDir + File.separator + dirName;
    }

    /**
     * 构造用户消息，如果存在质检失败结果则添加错误修复信息
     */
    private static String buildUserMessage(WorkflowContext context) {
        String userMessage = context.getEnhancedPrompt();

        // 检查是否存在质检失败结果
        QualityResult qualityResult = context.getQualityResult();
        if (isQualityCheckFailed(qualityResult)) {
            // 直接将错误修复信息作为新的提示词（起到了修改的作用）
            userMessage = buildErrorFixPrompt(context.getEnhancedPrompt(), qualityResult);
        }

        return userMessage;
    }

    /**
     * 判断质检是否失败
     */
    private static boolean isQualityCheckFailed(QualityResult qualityResult) {
        return qualityResult != null &&
                !qualityResult.getIsValid() &&
                qualityResult.getErrors() != null &&
                !qualityResult.getErrors().isEmpty();
    }

    /**
     * 构造错误修复提示词
     */
    private static String buildErrorFixPrompt(String originalPrompt, QualityResult qualityResult) {
        StringBuilder errorInfo = new StringBuilder();

        // 保留原始需求
        if (StrUtil.isNotBlank(originalPrompt)) {
            errorInfo.append("## 原始需求：\n").append(originalPrompt).append("\n\n");
        }

        errorInfo.append("## 上次生成的代码存在以下问题，请修复：\n");

        // 添加错误列表
        qualityResult.getErrors().forEach(error ->
                errorInfo.append("- ").append(error).append("\n"));

        // 添加修复建议（如果有）
        if (qualityResult.getSuggestions() != null && !qualityResult.getSuggestions().isEmpty()) {
            errorInfo.append("\n## 修复建议：\n");
            qualityResult.getSuggestions().forEach(suggestion ->
                    errorInfo.append("- ").append(suggestion).append("\n"));
        }

        errorInfo.append("\n请根据上述问题和建议重新生成代码，确保修复所有提到的问题。");

        return errorInfo.toString();
    }
}
