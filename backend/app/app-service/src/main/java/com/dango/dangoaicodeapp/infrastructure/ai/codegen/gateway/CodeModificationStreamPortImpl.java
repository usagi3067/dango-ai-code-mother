package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.CodeModificationStreamPort;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory.AiCodeModifierServiceFactory;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.AiCodeModifierService;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 代码修改流端口适配器。
 */
@Component
@RequiredArgsConstructor
public class CodeModificationStreamPortImpl implements CodeModificationStreamPort {

    private final AiCodeModifierServiceFactory aiCodeModifierServiceFactory;
    private final TokenStreamMessageFluxFactory tokenStreamMessageFluxFactory;

    @Override
    public Flux<String> modifyCodeStream(long appId, CodeGenTypeEnum codeGenType, String modifyRequest) {
        AiCodeModifierService service = aiCodeModifierServiceFactory.getModifierService(appId, codeGenType);
        TokenStream tokenStream = service.modifyVueProjectCodeStream(appId, modifyRequest);
        return tokenStreamMessageFluxFactory.toTextAndToolFlux(tokenStream);
    }
}

