package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.domain.codegen.port.IntentClassificationPort;
import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowMessagePort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 意图识别节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentClassifierNode {

    private static final String NODE_NAME = "意图识别";

    private final WorkflowMessagePort workflowMessagePort;
    private final IntentClassificationPort intentClassificationPort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            workflowMessagePort.emitNodeStart(context.getWorkflowExecutionId(), NODE_NAME);
            workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, "正在分析用户意图...\n");

            String userInput = context.getOriginalPrompt();
            String projectStructure = context.getProjectStructure();
            String classifyInput = String.format(
                "项目结构:\n%s\n\n用户输入:\n%s",
                projectStructure != null ? projectStructure : "无",
                userInput
            );

            String intent = intentClassificationPort.classify(classifyInput).trim().toUpperCase();
            if (!"MODIFY".equals(intent) && !"QA".equals(intent)) {
                log.warn("意图识别结果异常: {}，默认为 MODIFY", intent);
                intent = "MODIFY";
            }

            context.setIntentType(intent);
            log.info("意图识别结果: {}", intent);
            workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME,
                    String.format("识别为: %s\n", "QA".equals(intent) ? "问答模式" : "修改模式"));

            workflowMessagePort.emitNodeComplete(context.getWorkflowExecutionId(), NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }

    public String routeByIntent(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        String intent = context.getIntentType();
        if ("QA".equals(intent)) {
            return "qa";
        }
        return "modify";
    }
}
