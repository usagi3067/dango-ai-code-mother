package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.domain.codegen.ai.factory.AiIntentClassifierServiceFactory;
import com.dango.dangoaicodeapp.domain.codegen.ai.service.IntentClassifierService;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 意图识别节点
 * 根据用户输入判断意图为「修改代码」或「提问/答疑」，用于后续工作流路由
 *
 * @author dango
 */
@Slf4j
@Component
public class IntentClassifierNode {

    private static final String NODE_NAME = "意图识别";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在分析用户意图...\n");

            String userInput = context.getOriginalPrompt();
            String projectStructure = context.getProjectStructure();
            String classifyInput = String.format(
                "项目结构:\n%s\n\n用户输入:\n%s",
                projectStructure != null ? projectStructure : "无",
                userInput
            );

            AiIntentClassifierServiceFactory factory = SpringContextUtil.getBean(AiIntentClassifierServiceFactory.class);
            IntentClassifierService classifier = factory.createService();

            String intent = classifier.classify(classifyInput).trim().toUpperCase();
            if (!"MODIFY".equals(intent) && !"QA".equals(intent)) {
                log.warn("意图识别结果异常: {}，默认为 MODIFY", intent);
                intent = "MODIFY";
            }

            context.setIntentType(intent);
            log.info("意图识别结果: {}", intent);
            context.emitNodeMessage(NODE_NAME,
                    String.format("识别为: %s\n", "QA".equals(intent) ? "问答模式" : "修改模式"));

            context.emitNodeComplete(NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }

    public static String routeByIntent(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        String intent = context.getIntentType();
        if ("QA".equals(intent)) {
            return "qa";
        }
        return "modify";
    }
}
