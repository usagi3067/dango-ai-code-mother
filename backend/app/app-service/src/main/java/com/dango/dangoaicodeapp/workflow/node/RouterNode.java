package com.dango.dangoaicodeapp.workflow.node;

import com.dango.aicodegenerate.service.AiCodeGenTypeRoutingService;
import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 智能路由节点
 * 根据用户需求选择合适的代码生成类型
 */
@Slf4j
@Component
public class RouterNode {

    private static final String NODE_NAME = "智能路由";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            // 发送节点开始消息（使用 context 而非 ThreadLocal）
            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在分析需求，选择最佳代码生成类型...\n");

            CodeGenTypeEnum generationType;
            try {
                AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = SpringContextUtil.getBean(AiCodeGenTypeRoutingService.class);
                // 根据原始提示词进行智能路由
                generationType = aiCodeGenTypeRoutingService.routeCodeGenType(context.getOriginalPrompt());
                log.info("AI 智能路由完成，选择类型: {} ({})", 
                        generationType.getValue(), generationType.getText());
                context.emitNodeMessage(NODE_NAME, 
                        String.format("已选择生成类型: %s (%s)\n", generationType.getText(), generationType.getValue()));
            } catch (Exception e) {
                log.error("AI 智能路由失败，使用默认 HTML 类型: {}", e.getMessage());
                generationType = CodeGenTypeEnum.HTML;
                context.emitNodeMessage(NODE_NAME, 
                        "路由分析失败，使用默认 HTML 类型\n");
            }

            // 发送节点完成消息
            context.emitNodeComplete(NODE_NAME);

            // 更新状态
            context.setCurrentStep(NODE_NAME);
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}
