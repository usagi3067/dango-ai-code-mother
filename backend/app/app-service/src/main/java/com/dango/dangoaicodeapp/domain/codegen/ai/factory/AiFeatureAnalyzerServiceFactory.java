package com.dango.dangoaicodeapp.domain.codegen.ai.factory;

import com.dango.dangoaicodeapp.domain.codegen.ai.service.AiFeatureAnalyzerService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 功能分析服务工厂类
 * 使用默认的 ChatModel 进行结构化输出
 */
@Configuration
public class AiFeatureAnalyzerServiceFactory {

    @Resource
    private ChatModel chatModel;

    /**
     * 创建 AiFeatureAnalyzerService 实例
     * 使用默认的 ChatModel（已配置 JSON 格式）
     */
    @Bean
    public AiFeatureAnalyzerService aiFeatureAnalyzerService(ChatModel chatModel) {
        return AiServices.builder(AiFeatureAnalyzerService.class)
                .chatModel(chatModel)
                .build();
    }
}
