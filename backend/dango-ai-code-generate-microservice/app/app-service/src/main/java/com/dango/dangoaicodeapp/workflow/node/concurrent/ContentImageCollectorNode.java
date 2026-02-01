package com.dango.dangoaicodeapp.workflow.node.concurrent;

import com.dango.aicodegenerate.model.ImageCollectionPlan;
import com.dango.aicodegenerate.model.ImageResource;
import com.dango.aicodegenerate.tools.ImageSearchTool;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 内容图片收集节点
 * 并发执行内容图片搜索任务
 */
@Slf4j
public class ContentImageCollectorNode {

    private static final String NODE_NAME = "内容图片收集";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> contentImages = new ArrayList<>();

            // 发送节点开始消息
            context.emitNodeStart(NODE_NAME);

            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getContentImageTasks() != null && !plan.getContentImageTasks().isEmpty()) {
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);

                    log.info("开始并发收集内容图片，任务数: {}", plan.getContentImageTasks().size());
                    context.emitNodeMessage(NODE_NAME,
                            String.format("开始执行 %d 个搜索任务...\n", plan.getContentImageTasks().size()));

                    for (ImageCollectionPlan.ImageSearchTask task : plan.getContentImageTasks()) {
                        context.emitNodeMessage(NODE_NAME, String.format("搜索: %s\n", task.query()));
                        List<ImageResource> images = imageSearchTool.searchContentImages(task.query());
                        if (images != null) {
                            contentImages.addAll(images);
                        }
                    }

                    log.info("内容图片收集完成，共收集到 {} 张图片", contentImages.size());
                    context.emitNodeMessage(NODE_NAME,
                            String.format("收集完成，共 %d 张图片\n", contentImages.size()));
                } else {
                    context.emitNodeMessage(NODE_NAME, "无内容图片任务，跳过\n");
                }
            } catch (Exception e) {
                log.error("内容图片收集失败: {}", e.getMessage(), e);
                context.emitNodeError(NODE_NAME, e.getMessage());
            }

            // 将收集到的图片存储到上下文的中间字段中
            context.setContentImages(contentImages);
            context.setCurrentStep(NODE_NAME);
            context.emitNodeComplete(NODE_NAME);

            return WorkflowContext.saveContext(context);
        });
    }
}
