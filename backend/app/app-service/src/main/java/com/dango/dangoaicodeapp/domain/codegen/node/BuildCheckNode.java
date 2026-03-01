package com.dango.dangoaicodeapp.domain.codegen.node;

import cn.hutool.core.util.StrUtil;
import com.dango.aicodegenerate.model.QualityResult;
import com.dango.dangoaicodeapp.domain.codegen.model.ProjectBuildResult;
import com.dango.dangoaicodeapp.domain.codegen.port.ProjectBuildPort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 构建检查节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuildCheckNode {

    private static final String NODE_NAME = "构建检查";

    private final ProjectBuildPort projectBuildPort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            context.emitNodeStart(NODE_NAME);

            String generatedCodeDir = context.getGeneratedCodeDir();
            context.emitNodeMessage(NODE_NAME, "执行 npm install + npm run build...\n");

            QualityResult qualityResult;

            try {
                ProjectBuildResult buildResult = projectBuildPort.buildProject(generatedCodeDir);

                if (buildResult.success()) {
                    qualityResult = QualityResult.builder()
                            .isValid(true)
                            .build();

                    String buildResultDir = generatedCodeDir + File.separator + "dist";
                    context.setBuildResultDir(buildResultDir);

                    log.info("构建检查通过，dist 目录: {}", buildResultDir);
                    context.emitNodeMessage(NODE_NAME, "✅ 构建成功\n");
                } else {
                    String errorSummary = buildResult.errorSummary();
                    String stderr = buildResult.stderr();

                    List<String> errors = List.of(errorSummary);
                    List<String> suggestions = StrUtil.isNotBlank(stderr) ? List.of(stderr) : List.of();

                    qualityResult = QualityResult.builder()
                            .isValid(false)
                            .errors(errors)
                            .suggestions(suggestions)
                            .build();

                    log.warn("构建检查失败: {}", errorSummary);
                    context.emitNodeMessage(NODE_NAME,
                            "❌ 构建失败: " + errorSummary + "\n");
                }
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                log.error("构建检查异常: {}", errorMsg, e);
                qualityResult = QualityResult.builder()
                        .isValid(false)
                        .errors(List.of("构建检查异常: " + errorMsg))
                        .build();
                context.emitNodeError(NODE_NAME, errorMsg);
            }

            context.setQualityResult(qualityResult);
            context.emitNodeComplete(NODE_NAME);

            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
}
