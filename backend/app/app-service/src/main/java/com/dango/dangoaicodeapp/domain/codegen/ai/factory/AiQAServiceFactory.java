package com.dango.dangoaicodeapp.domain.codegen.ai.factory;

import com.dango.dangoaicodeapp.domain.codegen.ai.service.QAService;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 问答 AI 服务工厂
 */
@Component
public class AiQAServiceFactory {

    @Resource
    private StreamingChatModel odinaryStreamingChatModel;

    public QAService createService() {
        return AiServices.builder(QAService.class)
                .streamingChatModel(odinaryStreamingChatModel)
                .build();
    }
}
