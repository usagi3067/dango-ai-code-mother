package com.dango.dangoaicodeapp.infrastructure.ai.factory;

import com.dango.aicodegenerate.model.AiModelProvider;

import com.dango.dangoaicodeapp.infrastructure.ai.service.AiFeatureAnalyzerService;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 功能分析服务工厂类。
 */
@Configuration
public class AiFeatureAnalyzerServiceFactory {

    @Resource
    private AiModelProvider aiModelProvider;

    @Bean
    public AiFeatureAnalyzerService aiFeatureAnalyzerService() {
        return AiServices.builder(AiFeatureAnalyzerService.class)
                .chatModel(aiModelProvider.getChatModel("feature-analyzer"))
                .build();
    }
}
