package com.dango.dangoaicodeapp.domain.codegen.node.concurrent;

import com.dango.aicodegenerate.model.ImageCollectionPlan;
import com.dango.aicodegenerate.model.ImageResource;
import com.dango.dangoaicodeapp.domain.codegen.tools.MermaidDiagramTool;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 架构图绘制节点
 * 并发执行架构图生成任务
 */
@Slf4j
public class DiagramCollectorNode {

    private static final String NODE_NAME = "架构图生成";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> diagrams = new ArrayList<>();

            // 发送节点开始消息
            context.emitNodeStart(NODE_NAME);

            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getDiagramTasks() != null && !plan.getDiagramTasks().isEmpty()) {
                    MermaidDiagramTool diagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);

                    log.info("开始并发生成架构图，任务数: {}", plan.getDiagramTasks().size());
                    context.emitNodeMessage(NODE_NAME,
                            String.format("开始执行 %d 个生成任务...\n", plan.getDiagramTasks().size()));

                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        context.emitNodeMessage(NODE_NAME, String.format("生成: %s\n", task.description()));
                        List<ImageResource> images = diagramTool.generateMermaidDiagram(
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
