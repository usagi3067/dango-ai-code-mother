package com.dango.dangoaicodeapp.domain.codegen.node.concurrent;

import com.dango.aicodegenerate.model.ImageCollectionPlan;
import com.dango.aicodegenerate.model.ImageResource;
import com.dango.dangoaicodeapp.domain.codegen.tools.UndrawIllustrationTool;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 插画图片收集节点
 * 并发执行插画图片搜索任务
 */
@Slf4j
public class IllustrationCollectorNode {

    private static final String NODE_NAME = "插画图片收集";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> illustrations = new ArrayList<>();

            // 发送节点开始消息
            context.emitNodeStart(NODE_NAME);

            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getIllustrationTasks() != null && !plan.getIllustrationTasks().isEmpty()) {
                    UndrawIllustrationTool illustrationTool = SpringContextUtil.getBean(UndrawIllustrationTool.class);

                    log.info("开始并发收集插画图片，任务数: {}", plan.getIllustrationTasks().size());
                    context.emitNodeMessage(NODE_NAME,
                            String.format("开始执行 %d 个搜索任务...\n", plan.getIllustrationTasks().size()));

                    for (ImageCollectionPlan.IllustrationTask task : plan.getIllustrationTasks()) {
                        context.emitNodeMessage(NODE_NAME, String.format("搜索: %s\n", task.query()));
                        List<ImageResource> images = illustrationTool.searchIllustrations(task.query());
                        if (images != null) {
                            illustrations.addAll(images);
                        }
                    }

                    log.info("插画图片收集完成，共收集到 {} 张图片", illustrations.size());
                    context.emitNodeMessage(NODE_NAME,
                            String.format("收集完成，共 %d 张图片\n", illustrations.size()));
                } else {
                    context.emitNodeMessage(NODE_NAME, "无插画任务，跳过\n");
                }
            } catch (Exception e) {
                log.error("插画图片收集失败: {}", e.getMessage(), e);
                context.emitNodeError(NODE_NAME, e.getMessage());
            }

            context.setIllustrations(illustrations);
            context.setCurrentStep(NODE_NAME);
            context.emitNodeComplete(NODE_NAME);

            return WorkflowContext.saveContext(context);
        });
    }
}
