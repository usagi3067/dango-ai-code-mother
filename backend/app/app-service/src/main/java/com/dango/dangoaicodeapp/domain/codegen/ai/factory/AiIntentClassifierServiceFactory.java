package com.dango.dangoaicodeapp.domain.codegen.ai.factory;

import com.dango.dangoaicodeapp.domain.codegen.ai.service.IntentClassifierService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 意图识别 AI 服务工厂
 */
@Component
public class AiIntentClassifierServiceFactory {

    @Resource
    private ChatModel chatModel;

    public IntentClassifierService createService() {
        return AiServices.builder(IntentClassifierService.class)
                .chatModel(chatModel)
                .build();
    }
}
