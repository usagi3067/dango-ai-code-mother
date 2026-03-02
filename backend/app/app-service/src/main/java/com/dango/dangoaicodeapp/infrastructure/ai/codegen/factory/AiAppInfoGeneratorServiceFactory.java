package com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory;

import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.AiAppInfoGeneratorService;
import com.dango.aicodegenerate.model.AiModelProvider;

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
    private AiModelProvider aiModelProvider;

    /**
     * 创建 AiAppInfoGeneratorService 实例
     * 使用默认的 ChatModel（已配置 JSON 格式）
     */
    @Bean
    public AiAppInfoGeneratorService aiAppInfoGeneratorService() {
        return AiServices.builder(AiAppInfoGeneratorService.class)
                .chatModel(aiModelProvider.getChatModel("app-info-generator"))
                .build();
    }
}
