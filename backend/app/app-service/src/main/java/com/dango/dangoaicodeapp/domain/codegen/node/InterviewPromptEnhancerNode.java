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
            String advisorAdvice = context.getEnhancedPrompt();

            StringBuilder enhanced = new StringBuilder();
            enhanced.append(originalPrompt);
            enhanced.append("\n\n## 生成规范\n");
            enhanced.append("请严格按照 React + GSAP 模板的数据结构和组件规范生成代码。\n");
            enhanced.append("你只需要生成以下文件（其他文件已由模板提供，禁止修改）：\n");
            enhanced.append("- `src/data/question.js` — 问题元数据（topic、category、title、importance）\n");
            enhanced.append("- `src/diagrams/<name>/steps.js` — 动画步骤数据（state + speech + note）\n");
            enhanced.append("- `src/diagrams/<name>/Visualization.jsx` — GSAP 可视化组件\n");
            enhanced.append("- `src/diagrams/<name>/index.js` — 每个图解的聚合导出\n");
            enhanced.append("- `src/diagrams/index.js` — 图解注册\n");

            if (advisorAdvice != null && !advisorAdvice.isBlank()) {
                enhanced.append("\n## 图解设计建议（由 AI 分析生成，请严格参考）\n");
                enhanced.append(advisorAdvice);
            }

            context.setEnhancedPrompt(enhanced.toString());

            context.emitNodeMessage(NODE_NAME, "提示词构建完成\n");
            context.emitNodeComplete(NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
}
