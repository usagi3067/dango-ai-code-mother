package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Component
public class InterviewPromptEnhancerNode {

    private static final String NODE_NAME = "面试题解提示词增强";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在构建面试题解生成提示词...\n");

            String originalPrompt = context.getOriginalPrompt();

            StringBuilder enhanced = new StringBuilder();
            enhanced.append(originalPrompt);
            enhanced.append("\n\n## 生成规范\n");
            enhanced.append("请严格按照面试题解可视化模板的数据结构和组件规范生成代码。\n");
            enhanced.append("你只需要生成以下文件（其他文件已由模板提供，禁止修改）：\n");
            enhanced.append("- `src/data/topic.ts` — 题目数据（概念结构、分类、要点）\n");
            enhanced.append("- `src/data/explanation.ts` — 讲解数据（对比项、流程步骤、回答指南）\n");
            enhanced.append("- `src/components/visualizations/*.vue` — 可视化组件\n");

            context.setEnhancedPrompt(enhanced.toString());

            context.emitNodeMessage(NODE_NAME, "提示词构建完成\n");
            context.emitNodeComplete(NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
}
