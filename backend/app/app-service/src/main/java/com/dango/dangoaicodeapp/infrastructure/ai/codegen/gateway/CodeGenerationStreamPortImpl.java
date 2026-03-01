package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.CodeGenerationStreamPort;
import com.dango.dangoaicodeapp.domain.codegen.service.AiCodeGeneratorFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 代码生成流端口适配器。
 */
@Component
@RequiredArgsConstructor
public class CodeGenerationStreamPortImpl implements CodeGenerationStreamPort {

    private final AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Override
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(userMessage, codeGenTypeEnum, appId);
    }
}

