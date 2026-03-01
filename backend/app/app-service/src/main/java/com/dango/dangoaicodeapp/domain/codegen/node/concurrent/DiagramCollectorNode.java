package com.dango.dangoaicodeapp.domain.codegen.node.concurrent;

import com.dango.aicodegenerate.model.ImageCollectionPlan;
import com.dango.aicodegenerate.model.ImageResource;
import com.dango.dangoaicodeapp.domain.codegen.port.ImageResourcePort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 架构图绘制节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiagramCollectorNode {

    private static final String NODE_NAME = "架构图生成";

    private final ImageResourcePort imageResourcePort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> diagrams = new ArrayList<>();

            context.emitNodeStart(NODE_NAME);

            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getDiagramTasks() != null && !plan.getDiagramTasks().isEmpty()) {
                    log.info("开始并发生成架构图，任务数: {}", plan.getDiagramTasks().size());
                    context.emitNodeMessage(NODE_NAME,
                            String.format("开始执行 %d 个生成任务...\n", plan.getDiagramTasks().size()));

                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        context.emitNodeMessage(NODE_NAME, String.format("生成: %s\n", task.description()));
                        List<ImageResource> images = imageResourcePort.generateMermaidDiagram(
                                task.mermaidCode(), task.description());
                        if (images != null) {
                            diagrams.addAll(images);
                        }
                    }

                    log.info("架构图生成完成，共生成 {} 张图片", diagrams.size());
                    context.emitNodeMessage(NODE_NAME,
                            String.format("生成完成，共 %d 张图片\n", diagrams.size()));
                } else {
                    context.emitNodeMessage(NODE_NAME, "无架构图任务，跳过\n");
                }
            } catch (Exception e) {
                log.error("架构图生成失败: {}", e.getMessage(), e);
                context.emitNodeError(NODE_NAME, e.getMessage());
            }

            context.setDiagrams(diagrams);
            context.setCurrentStep(NODE_NAME);
            context.emitNodeComplete(NODE_NAME);

            return WorkflowContext.saveContext(context);
        });
    }
}
