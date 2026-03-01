package com.dango.dangoaicodeapp.domain.codegen.node.concurrent;

import com.dango.aicodegenerate.model.ImageCollectionPlan;
import com.dango.aicodegenerate.model.ImageResource;
import com.dango.dangoaicodeapp.domain.codegen.port.ImageResourcePort;
import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowMessagePort;
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
 * 内容图片收集节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentImageCollectorNode {

    private static final String NODE_NAME = "内容图片收集";

    private final WorkflowMessagePort workflowMessagePort;
    private final ImageResourcePort imageResourcePort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> contentImages = new ArrayList<>();

            workflowMessagePort.emitNodeStart(context.getWorkflowExecutionId(), NODE_NAME);

            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getContentImageTasks() != null && !plan.getContentImageTasks().isEmpty()) {
                    log.info("开始并发收集内容图片，任务数: {}", plan.getContentImageTasks().size());
                    workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME,
                            String.format("开始执行 %d 个搜索任务...\n", plan.getContentImageTasks().size()));

                    for (ImageCollectionPlan.ImageSearchTask task : plan.getContentImageTasks()) {
                        workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, String.format("搜索: %s\n", task.query()));
                        List<ImageResource> images = imageResourcePort.searchContentImages(task.query());
                        if (images != null) {
                            contentImages.addAll(images);
                        }
                    }

                    log.info("内容图片收集完成，共收集到 {} 张图片", contentImages.size());
                    workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME,
                            String.format("收集完成，共 %d 张图片\n", contentImages.size()));
                } else {
                    workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, "无内容图片任务，跳过\n");
                }
            } catch (Exception e) {
                log.error("内容图片收集失败: {}", e.getMessage(), e);
                workflowMessagePort.emitNodeError(context.getWorkflowExecutionId(), NODE_NAME, e.getMessage());
            }

            context.setContentImages(contentImages);
            context.setCurrentStep(NODE_NAME);
            workflowMessagePort.emitNodeComplete(context.getWorkflowExecutionId(), NODE_NAME);

            return WorkflowContext.saveContext(context);
        });
    }
}
