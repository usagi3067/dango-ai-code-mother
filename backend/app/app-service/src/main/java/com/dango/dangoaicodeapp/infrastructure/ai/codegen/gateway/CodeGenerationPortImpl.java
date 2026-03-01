package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.CodeGenerationPort;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory.AiCodeGeneratorServiceFactory;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.CodeGeneratorService;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CodeGenerationPortImpl implements CodeGenerationPort {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Override
    public TokenStream generateCodeStream(long appId, CodeGenTypeEnum codeGenType, String userMessage) {
        CodeGeneratorService service = aiCodeGeneratorServiceFactory.getService(appId, codeGenType);
        return service.generateCodeStream(appId, userMessage);
    }
}
