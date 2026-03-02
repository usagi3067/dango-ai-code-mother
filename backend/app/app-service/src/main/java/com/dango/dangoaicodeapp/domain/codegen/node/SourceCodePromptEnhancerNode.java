package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowMessagePort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
@Component
@RequiredArgsConstructor
public class SourceCodePromptEnhancerNode {

    private static final String NODE_NAME = "源码讲解提示词增强";

    private final WorkflowMessagePort workflowMessagePort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            workflowMessagePort.emitNodeStart(context.getWorkflowExecutionId(), NODE_NAME);
            workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, "正在构建源码讲解生成提示词...\n");

            String originalPrompt = context.getOriginalPrompt();
            String advisorAdvice = context.getEnhancedPrompt();

            StringBuilder enhanced = new StringBuilder();
            enhanced.append(originalPrompt);

            enhanced.append("\n\n## 生成规范\n");
            enhanced.append("请严格按照源码剧场模板的数据结构规范生成代码。\n");
            enhanced.append("你只需要生成以下文件（其他文件已由模板提供，禁止修改）：\n");
            enhanced.append("- `src/data/question.js` — 问题元数据（topic、category、title、importance）\n");
            enhanced.append("- `src/diagrams/<name>/steps.js` — 源码讲解步骤数据（file + code + highlightLines + annotations + speech）\n");
            enhanced.append("- `src/diagrams/<name>/index.js` — 每个讲解的聚合导出（无 Visualization 引用）\n");
            enhanced.append("- `src/diagrams/index.js` — 讲解注册\n");
            enhanced.append("\n⚠️ 不需要生成 Visualization.jsx，源码展示由模板预置的 CodeViewer 组件处理。\n");

            if (advisorAdvice != null && !advisorAdvice.isBlank()) {
                enhanced.append("\n## 源码讲解设计建议（由 AI 分析生成，请严格参考）\n");
                enhanced.append(advisorAdvice);
            }

            context.setEnhancedPrompt(enhanced.toString());

            workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, "提示词构建完成\n");
            workflowMessagePort.emitNodeComplete(context.getWorkflowExecutionId(), NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
}
