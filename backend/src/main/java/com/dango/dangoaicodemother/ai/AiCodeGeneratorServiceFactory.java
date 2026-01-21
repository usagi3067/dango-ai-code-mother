package com.dango.dangoaicodemother.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    /**
     * 创建 AiCodeGeneratorService 实例。
     * 这里同时配置了两个模型：
     * 1. chatModel: 用于执行阻塞式的（非流式）AI 调用。当你需要一次性获取完整结果（例如简单的文本生成）时，AiServices 会自动使用此模型。
     * 2. streamingChatModel: 用于执行流式 AI 调用。当你需要实时获取 AI 生成的内容（例如像 ChatGPT 那样逐字输出）并返回 Flux 或以监听器方式处理时，AiServices 会自动切换到此模型。
     * 
     * 通过在 AiServices 中同时配置这两者，同一个服务接口既支持同步调用，也支持响应式/流式调用。
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}
