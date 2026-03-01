package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.app.valueobject.OperationModeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.ProjectWorkspacePort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码读取节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeReaderNode {

    private static final String NODE_NAME = "代码读取";

    private final ProjectWorkspacePort projectWorkspacePort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在读取项目结构...\n");

            Long appId = context.getAppId();
            CodeGenTypeEnum generationType = context.getGenerationType();
            String projectStructure = projectWorkspacePort.readProjectStructure(appId, generationType);

            if (projectStructure == null || projectStructure.isEmpty()) {
                context.setOperationMode(OperationModeEnum.CREATE);
                log.warn("未找到现有项目，切换到创建模式");
                context.emitNodeMessage(NODE_NAME, "未找到现有项目代码，将切换到创建模式\n");
            } else {
                context.setProjectStructure(projectStructure);
                log.info("项目结构读取完成");
                context.emitNodeMessage(NODE_NAME, "项目结构读取完成\n");
            }

            context.emitNodeComplete(NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
}
