package com.dango.dangoaicodeapp.workflow.node;

import cn.hutool.core.util.StrUtil;
import com.dango.aicodegenerate.model.QualityResult;
import com.dango.dangoaicodeapp.core.builder.VueProjectBuilder;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 构建检查节点
 * 执行 npm install + npm run build，用真实编译器错误驱动修复循环
 * 替代原有的 CodeQualityCheckNode（AI 质检）+ ProjectBuilderNode（构建）
 */
@Slf4j
@Component
public class BuildCheckNode {

    private static final String NODE_NAME = "构建检查";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            context.emitNodeStart(NODE_NAME);

            String generatedCodeDir = context.getGeneratedCodeDir();

            context.emitNodeMessage(NODE_NAME, "执行 npm install + npm run build...\n");

            QualityResult qualityResult;

            try {
                VueProjectBuilder vueProjectBuilder = SpringContextUtil.getBean(VueProjectBuilder.class);
                VueProjectBuilder.BuildResult buildResult = vueProjectBuilder.buildProjectWithResult(generatedCodeDir);

                if (buildResult.isSuccess()) {
                    qualityResult = QualityResult.builder()
                            .isValid(true)
                            .build();

                    String buildResultDir = generatedCodeDir + File.separator + "dist";
                    context.setBuildResultDir(buildResultDir);

                    log.info("构建检查通过，dist 目录: {}", buildResultDir);
                    context.emitNodeMessage(NODE_NAME, "✅ 构建成功\n");
                } else {
                    String errorSummary = buildResult.getErrorSummary();
                    String stderr = buildResult.getStderr();

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
                log.error("构建检查异常: {}", e.getMessage(), e);
                qualityResult = QualityResult.builder()
                        .isValid(false)
                        .errors(List.of("构建检查异常: " + e.getMessage()))
                        .build();
                context.emitNodeError(NODE_NAME, e.getMessage());
            }

            context.setQualityResult(qualityResult);
            context.emitNodeComplete(NODE_NAME);

            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
}
