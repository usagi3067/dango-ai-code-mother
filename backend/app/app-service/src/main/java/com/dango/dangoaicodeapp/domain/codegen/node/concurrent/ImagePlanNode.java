package com.dango.dangoaicodeapp.domain.codegen.node.concurrent;

import com.dango.aicodegenerate.model.ImageCollectionPlan;
import com.dango.dangoaicodeapp.domain.codegen.port.ImageCollectionPort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片计划节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImagePlanNode {

    private static final String NODE_NAME = "图片规划";

    private final ImageCollectionPort imageCollectionPort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();

            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在分析需求，规划图片收集任务...\n");

            try {
                ImageCollectionPlan plan = imageCollectionPort.planImageCollection(originalPrompt);

                log.info("生成图片收集计划，准备启动并发分支");
                context.setImageCollectionPlan(plan);
                context.setCurrentStep(NODE_NAME);

                int contentCount = plan.getContentImageTasks() != null ? plan.getContentImageTasks().size() : 0;
                int illustrationCount = plan.getIllustrationTasks() != null ? plan.getIllustrationTasks().size() : 0;
                int diagramCount = plan.getDiagramTasks() != null ? plan.getDiagramTasks().size() : 0;
                int logoCount = plan.getLogoTasks() != null ? plan.getLogoTasks().size() : 0;

                context.emitNodeMessage(NODE_NAME,
                        String.format("计划完成：内容图片 %d 个任务，插画 %d 个任务，架构图 %d 个任务，Logo %d 个任务\n",
                                contentCount, illustrationCount, diagramCount, logoCount));

            } catch (Exception e) {
                log.error("图片计划生成失败: {}", e.getMessage(), e);
                context.emitNodeError(NODE_NAME, e.getMessage());
            }

            context.emitNodeComplete(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
}
