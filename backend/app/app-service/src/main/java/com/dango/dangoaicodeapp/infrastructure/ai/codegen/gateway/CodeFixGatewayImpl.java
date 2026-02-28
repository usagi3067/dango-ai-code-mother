package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.CodeFixGateway;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory.AiCodeFixerServiceFactory;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.CodeFixerService;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CodeFixGatewayImpl implements CodeFixGateway {

    @Resource
    private AiCodeFixerServiceFactory aiCodeFixerServiceFactory;

    @Override
    public TokenStream fixCodeStream(long appId, CodeGenTypeEnum codeGenType, String fixRequest) {
        CodeFixerService service = aiCodeFixerServiceFactory.getFixerService(appId, codeGenType);
        return service.fixCodeStream(appId, fixRequest);
    }
}
