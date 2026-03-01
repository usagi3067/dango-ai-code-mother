package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.domain.codegen.model.ProjectBuildResult;
import com.dango.dangoaicodeapp.domain.codegen.port.ProjectBuildPort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.io.File;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 项目构建节点（兼容旧流程，当前主流程使用 BuildCheckNode）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectBuilderNode {

    private static final String NODE_NAME = "项目构建";

    private final ProjectBuildPort projectBuildPort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            context.emitNodeStart(NODE_NAME);

            String generatedCodeDir = context.getGeneratedCodeDir();
            String buildResultDir;

            context.emitNodeMessage(NODE_NAME, "检测到 Vue 项目，开始执行构建...\n");
            context.emitNodeMessage(NODE_NAME, "执行 npm install...\n");

            try {
                ProjectBuildResult result = projectBuildPort.buildProject(generatedCodeDir);
                if (result.success()) {
                    buildResultDir = generatedCodeDir + File.separator + "dist";
                    log.info("Vue 项目构建成功，dist 目录: {}", buildResultDir);
                    context.emitNodeMessage(NODE_NAME, "npm run build 执行成功\n");
                    context.emitNodeMessage(NODE_NAME,
                            String.format("构建产物目录: %s\n", buildResultDir));
                } else {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                            "Vue 项目构建失败: " + result.errorSummary());
                }
            } catch (BusinessException e) {
                context.emitNodeError(NODE_NAME, e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Vue 项目构建异常: {}", e.getMessage(), e);
                context.setErrorMessage("Vue 项目构建异常: " + e.getMessage());
                context.emitNodeError(NODE_NAME, e.getMessage());
                buildResultDir = generatedCodeDir;
            }

            context.emitNodeComplete(NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            context.setBuildResultDir(buildResultDir);
            log.info("项目构建节点完成，最终目录: {}", buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }
}
