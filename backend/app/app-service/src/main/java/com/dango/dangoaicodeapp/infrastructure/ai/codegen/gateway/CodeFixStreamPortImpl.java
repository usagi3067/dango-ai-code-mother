package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.CodeFixStreamPort;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory.AiCodeFixerServiceFactory;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.CodeFixerService;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 代码修复流端口适配器。
 */
@Component
@RequiredArgsConstructor
public class CodeFixStreamPortImpl implements CodeFixStreamPort {

    private final AiCodeFixerServiceFactory aiCodeFixerServiceFactory;
    private final TokenStreamMessageFluxFactory tokenStreamMessageFluxFactory;

    @Override
    public Flux<String> fixCodeStream(long appId, CodeGenTypeEnum codeGenType, String fixRequest) {
        CodeFixerService service = aiCodeFixerServiceFactory.getFixerService(appId, codeGenType);
        TokenStream tokenStream = service.fixCodeStream(appId, fixRequest);
        return tokenStreamMessageFluxFactory.toTextAndToolFlux(tokenStream);
    }
}

