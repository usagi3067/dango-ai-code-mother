package com.dango.dangoaicodeapp.workflow.node.concurrent;

import com.dango.aicodegenerate.model.ImageCollectionPlan;
import com.dango.aicodegenerate.service.ImageCollectionPlanService;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片计划节点
 * 分析用户需求，生成图片收集计划，为并发执行做准备
 */
@Slf4j
public class ImagePlanNode {

    private static final String NODE_NAME = "图片规划";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();

            // 发送节点开始消息
            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在分析需求，规划图片收集任务...\n");

            try {
                // 获取图片收集计划服务
                ImageCollectionPlanService planService = SpringContextUtil.getBean(ImageCollectionPlanService.class);
                ImageCollectionPlan plan = planService.planImageCollection(originalPrompt);

                log.info("生成图片收集计划，准备启动并发分支");

                // 将计划存储到上下文中
                context.setImageCollectionPlan(plan);
                context.setCurrentStep(NODE_NAME);

                // 输出计划摘要
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
