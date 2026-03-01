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
 * 模式路由节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModeRouterNode {

    private static final String NODE_NAME = "模式路由";

    private final ProjectWorkspacePort projectWorkspacePort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在分析操作模式...\n");

            OperationModeEnum mode = determineOperationMode(context);
            context.setOperationMode(mode);

            log.info("操作模式判断完成: {} ({})", mode.getValue(), mode.getText());
            context.emitNodeMessage(NODE_NAME,
                    String.format("已确定操作模式: %s (%s)\n", mode.getText(), mode.getValue()));

            context.emitNodeComplete(NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }

    private OperationModeEnum determineOperationMode(WorkflowContext context) {
        if (context.getElementInfo() != null) {
            log.info("检测到 elementInfo，使用已有代码模式");
            return OperationModeEnum.EXISTING_CODE;
        }

        if (projectWorkspacePort.hasExistingCode(context.getAppId(), context.getGenerationType())) {
            log.info("检测到历史代码，使用已有代码模式");
            return OperationModeEnum.EXISTING_CODE;
        }

        log.info("未检测到历史代码，使用创建模式");
        return OperationModeEnum.CREATE;
    }

    public String routeToNextNode(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        OperationModeEnum mode = context.getOperationMode();

        if (mode == null) {
            log.warn("操作模式为空，默认路由到创建模式");
            return "create";
        }

        return switch (mode) {
            case CREATE -> {
                if (context.getGenerationType() == CodeGenTypeEnum.LEETCODE_PROJECT) {
                    log.info("力扣题解类型，路由到力扣创建子图");
                    yield "leetcode_create";
                }
                if (context.getGenerationType() == CodeGenTypeEnum.INTERVIEW_PROJECT) {
                    log.info("面试题解类型，路由到面试创建子图");
                    yield "interview_create";
                }
                yield "create";
            }
            case EXISTING_CODE -> {
                log.info("已有代码，路由到已有代码子图（意图识别）");
                yield "existing_code";
            }
            case MODIFY -> "modify";
            case FIX -> "create";
            case QA -> "existing_code";
        };
    }
}
