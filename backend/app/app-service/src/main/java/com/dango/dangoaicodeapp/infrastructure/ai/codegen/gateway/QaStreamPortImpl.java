package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.dangoaicodeapp.domain.codegen.port.QaStreamPort;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory.AiQAServiceFactory;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.QAService;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 问答流端口适配器。
 */
@Component
@RequiredArgsConstructor
public class QaStreamPortImpl implements QaStreamPort {

    private final AiQAServiceFactory aiQaServiceFactory;
    private final TokenStreamMessageFluxFactory tokenStreamMessageFluxFactory;

    @Override
    public Flux<String> answer(long appId, String qaInput) {
        QAService qaService = aiQaServiceFactory.createService(appId);
        TokenStream tokenStream = qaService.answer(appId, qaInput);
        return tokenStreamMessageFluxFactory.toTextFlux(tokenStream);
    }
}

