package com.dango.dangoaicodeapp.domain.codegen.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.dango.aicodegenerate.model.ImageResource;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 提示词增强节点
 * 将图片资源信息整合到提示词中
 */
@Slf4j
@Component
public class PromptEnhancerNode {

    private static final String NODE_NAME = "提示词增强";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            // 发送节点开始消息（使用 context 而非 ThreadLocal）
            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在整合图片资源到提示词中...\n");

            // 获取原始提示词和图片列表
            String originalPrompt = context.getOriginalPrompt();
            String imageListStr = context.getImageListStr();
            List<ImageResource> imageList = context.getImageList();

            // 构建增强后的提示词
            StringBuilder enhancedPromptBuilder = new StringBuilder();
            enhancedPromptBuilder.append(originalPrompt);

            // 如果有图片资源，则添加图片信息
            if (CollUtil.isNotEmpty(imageList) || StrUtil.isNotBlank(imageListStr)) {
                enhancedPromptBuilder.append("\n\n## 可用素材资源\n");
                enhancedPromptBuilder.append("请在生成网站时使用以下图片资源，将这些图片合理地嵌入到网站的相应位置中。\n");

                if (CollUtil.isNotEmpty(imageList)) {
                    for (ImageResource image : imageList) {
                        enhancedPromptBuilder.append("- ")
                                .append(image.getCategory().getText())
                                .append("：")
                                .append(image.getDescription())
                                .append("（")
                                .append(image.getUrl())
                                .append("）\n");
                    }
                    context.emitNodeMessage(NODE_NAME,
                            String.format("已整合 %d 个图片资源\n", imageList.size()));
                } else {
                    enhancedPromptBuilder.append(imageListStr);
                    context.emitNodeMessage(NODE_NAME, "已整合图片资源信息\n");
                }
            } else {
                context.emitNodeMessage(NODE_NAME, "无图片资源需要整合\n");
            }

            String enhancedPrompt = enhancedPromptBuilder.toString();

            // 发送节点完成消息
            context.emitNodeComplete(NODE_NAME);

            // 更新状态
            context.setCurrentStep(NODE_NAME);
            context.setEnhancedPrompt(enhancedPrompt);
            log.info("提示词增强完成，增强后长度: {} 字符", enhancedPrompt.length());
            return WorkflowContext.saveContext(context);
        });
    }
}
