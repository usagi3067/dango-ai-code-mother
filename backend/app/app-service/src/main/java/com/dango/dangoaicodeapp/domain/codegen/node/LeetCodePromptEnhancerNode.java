package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 力扣题解提示词增强节点
 * 为力扣题解生成构建专用提示词，指定生成规范和文件范围
 *
 * @author dango
 */
@Slf4j
@Component
public class LeetCodePromptEnhancerNode {

    private static final String NODE_NAME = "力扣提示词增强";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在构建力扣题解生成提示词...\n");

            String originalPrompt = context.getOriginalPrompt();

            StringBuilder enhanced = new StringBuilder();
            enhanced.append(originalPrompt);
            enhanced.append("\n\n## 生成规范\n");
            enhanced.append("请严格按照 leetcode-animation 模板的数据结构和组件规范生成代码。\n");
            enhanced.append("你只需要生成以下文件（其他文件已由模板提供，禁止修改）：\n");
            enhanced.append("- `src/data/problem.ts` — 题目数据（题号、标题、描述、核心思路、解法对比）\n");
            enhanced.append("- `src/data/solutions.ts` — 解法数据（每个解法的代码、动画步骤、可视化状态）\n");
            enhanced.append("- `src/components/visualizations/*.vue` — 每个解法对应的可视化组件\n");

            context.setEnhancedPrompt(enhanced.toString());

            context.emitNodeMessage(NODE_NAME, "提示词构建完成\n");
            context.emitNodeComplete(NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
}
