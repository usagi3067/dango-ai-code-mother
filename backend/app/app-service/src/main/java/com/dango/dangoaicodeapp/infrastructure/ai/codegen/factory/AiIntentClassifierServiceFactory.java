package com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory;

import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.IntentClassifierService;
import com.dango.aicodegenerate.model.AiModelProvider;

import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 意图识别 AI 服务工厂
 */
@Component
public class AiIntentClassifierServiceFactory {

    @Resource
    private AiModelProvider aiModelProvider;

    public IntentClassifierService createService() {
        return AiServices.builder(IntentClassifierService.class)
                .chatModel(aiModelProvider.getChatModel("intent-classifier"))
                .build();
    }
}
