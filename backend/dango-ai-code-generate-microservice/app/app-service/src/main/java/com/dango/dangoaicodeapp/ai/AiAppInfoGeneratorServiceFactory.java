package com.dango.dangoaicodeapp.ai;

import com.dango.aicodegenerate.service.AiAppInfoGeneratorService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 应用信息生成服务工厂类
 * 使用默认的 ChatModel 进行结构化输出
 */
@Configuration
public class AiAppInfoGeneratorServiceFactory {

    @Resource
    private ChatModel openAiChatModel;

    /**
     * 创建 AiAppInfoGeneratorService 实例
     * 使用默认的 ChatModel（已配置 JSON 格式）
     */
    @Bean
    public AiAppInfoGeneratorService aiAppInfoGeneratorService(ChatModel chatModel) {
        return AiServices.builder(AiAppInfoGeneratorService.class)
                .chatModel(openAiChatModel)
                .build();
    }
}
