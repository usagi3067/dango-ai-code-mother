package com.dango.dangoaicodeapp.domain.codegen.ai.factory;

import com.dango.dangoaicodeapp.domain.codegen.ai.service.CodeQualityCheckService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 代码质量检查 AI 服务工厂
 */
@Slf4j
@Configuration
public class CodeQualityCheckServiceFactory {

    @Resource
    private ChatModel chatModel;

    /**
     * 创建代码质量检查 AI 服务
     */
    @Bean
    public CodeQualityCheckService codeQualityCheckService() {
        log.info("创建代码质量检查 AI 服务");
        return AiServices.builder(CodeQualityCheckService.class)
                .chatModel(chatModel)
                .build();
    }
}
