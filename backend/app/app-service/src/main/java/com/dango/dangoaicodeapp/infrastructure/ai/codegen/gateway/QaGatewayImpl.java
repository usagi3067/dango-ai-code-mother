package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.dangoaicodeapp.domain.codegen.port.QaGateway;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory.AiQAServiceFactory;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.QAService;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class QaGatewayImpl implements QaGateway {

    @Resource
    private AiQAServiceFactory aiQaServiceFactory;

    @Override
    public TokenStream answer(long appId, String qaInput) {
        QAService qaService = aiQaServiceFactory.createService(appId);
        return qaService.answer(appId, qaInput);
    }
}
