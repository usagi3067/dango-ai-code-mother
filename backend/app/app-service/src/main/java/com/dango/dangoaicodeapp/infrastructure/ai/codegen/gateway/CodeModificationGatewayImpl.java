package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.CodeModificationGateway;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory.AiCodeModifierServiceFactory;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.AiCodeModifierService;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CodeModificationGatewayImpl implements CodeModificationGateway {

    @Resource
    private AiCodeModifierServiceFactory aiCodeModifierServiceFactory;

    @Override
    public TokenStream modifyCodeStream(long appId, CodeGenTypeEnum codeGenType, String modifyRequest) {
        AiCodeModifierService service = aiCodeModifierServiceFactory.getModifierService(appId, codeGenType);
        return service.modifyVueProjectCodeStream(appId, modifyRequest);
    }
}
