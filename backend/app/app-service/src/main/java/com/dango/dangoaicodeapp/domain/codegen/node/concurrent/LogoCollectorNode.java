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
 * Logo 生成节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogoCollectorNode {

    private static final String NODE_NAME = "Logo生成";

    private final WorkflowMessagePort workflowMessagePort;
    private final ImageResourcePort imageResourcePort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> logos = new ArrayList<>();

            workflowMessagePort.emitNodeStart(context.getWorkflowExecutionId(), NODE_NAME);

            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getLogoTasks() != null && !plan.getLogoTasks().isEmpty()) {
                    log.info("开始并发生成Logo，任务数: {}", plan.getLogoTasks().size());
                    workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME,
                            String.format("开始执行 %d 个生成任务...\n", plan.getLogoTasks().size()));

                    for (ImageCollectionPlan.LogoTask task : plan.getLogoTasks()) {
                        workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, String.format("生成: %s\n", task.description()));
                        List<ImageResource> images = imageResourcePort.generateLogos(task.description());
                        if (images != null) {
                            logos.addAll(images);
                        }
                    }

                    log.info("Logo生成完成，共生成 {} 张图片", logos.size());
                    workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME,
                            String.format("生成完成，共 %d 张图片\n", logos.size()));
                } else {
                    workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, "无Logo任务，跳过\n");
                }
            } catch (Exception e) {
                log.error("Logo生成失败: {}", e.getMessage(), e);
                workflowMessagePort.emitNodeError(context.getWorkflowExecutionId(), NODE_NAME, e.getMessage());
            }

            context.setLogos(logos);
            context.setCurrentStep(NODE_NAME);
            workflowMessagePort.emitNodeComplete(context.getWorkflowExecutionId(), NODE_NAME);

            return WorkflowContext.saveContext(context);
        });
    }
}
