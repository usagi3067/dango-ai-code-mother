package com.dango.dangoaicodeapp.workflow.node.concurrent;

import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.model.ImageResource;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片聚合节点
 * 汇聚所有并发分支收集到的图片
 */
@Slf4j
public class ImageAggregatorNode {

    private static final String NODE_NAME = "图片聚合";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> allImages = new ArrayList<>();

            // 发送节点开始消息
            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在聚合并发收集的图片...\n");

            log.info("开始聚合并发收集的图片");

            // 从各个中间字段聚合图片
            int contentCount = 0, illustrationCount = 0, diagramCount = 0, logoCount = 0;

            if (context.getContentImages() != null) {
                allImages.addAll(context.getContentImages());
                contentCount = context.getContentImages().size();
            }
            if (context.getIllustrations() != null) {
                allImages.addAll(context.getIllustrations());
                illustrationCount = context.getIllustrations().size();
            }
            if (context.getDiagrams() != null) {
                allImages.addAll(context.getDiagrams());
                diagramCount = context.getDiagrams().size();
            }
            if (context.getLogos() != null) {
                allImages.addAll(context.getLogos());
                logoCount = context.getLogos().size();
            }

            log.info("图片聚合完成，总共 {} 张图片", allImages.size());

            // 输出聚合统计
            context.emitNodeMessage(NODE_NAME,
                    String.format("聚合完成：内容图片 %d 张，插画 %d 张，架构图 %d 张，Logo %d 张，总计 %d 张\n",
                            contentCount, illustrationCount, diagramCount, logoCount, allImages.size()));

            // 更新最终的图片列表
            context.setImageList(allImages);
            // 同时生成图片列表字符串，供后续节点使用
            context.setImageListStr(JSONUtil.toJsonStr(allImages));
            context.setCurrentStep(NODE_NAME);

            context.emitNodeComplete(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }
}
