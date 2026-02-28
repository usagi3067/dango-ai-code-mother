package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.dangoaicodeapp.domain.codegen.port.IntentClassificationGateway;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory.AiIntentClassifierServiceFactory;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.IntentClassifierService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class IntentClassificationGatewayImpl implements IntentClassificationGateway {

    @Resource
    private AiIntentClassifierServiceFactory aiIntentClassifierServiceFactory;

    @Override
    public String classify(String classifyInput) {
        IntentClassifierService service = aiIntentClassifierServiceFactory.createService();
        return service.classify(classifyInput);
    }
}
