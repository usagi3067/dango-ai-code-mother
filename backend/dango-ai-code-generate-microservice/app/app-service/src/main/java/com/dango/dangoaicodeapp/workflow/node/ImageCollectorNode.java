package com.dango.dangoaicodeapp.workflow.node;

import com.dango.aicodegenerate.service.ImageCollectionService;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片收集节点
 * 使用 AI 进行工具调用，收集不同类型的图片
 */
@Slf4j
@Component
public class ImageCollectorNode {

    private static final String NODE_NAME = "图片收集";

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            String imageListStr = "";

            // 发送节点开始消息（使用 context 而非 ThreadLocal）
            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在分析需求并收集相关图片资源...\n");
            
            try {
                ImageCollectionService imageCollectionService = SpringContextUtil.getBean(ImageCollectionService.class);
                // 使用 AI 服务进行智能图片收集
                imageListStr = imageCollectionService.collectImages(originalPrompt);
                log.info("图片收集完成，结果长度: {} 字符", imageListStr.length());
                context.emitNodeMessage(NODE_NAME, 
                        String.format("收集到 %d 字符的图片资源信息\n", imageListStr.length()));
            } catch (Exception e) {
                log.error("图片收集失败: {}", e.getMessage(), e);
                context.emitNodeError(NODE_NAME, e.getMessage());
            }
            
            // 发送节点完成消息
            context.emitNodeComplete(NODE_NAME);
            
            // 更新状态
            context.setCurrentStep(NODE_NAME);
            context.setImageListStr(imageListStr);
            return WorkflowContext.saveContext(context);
        });
    }
}
