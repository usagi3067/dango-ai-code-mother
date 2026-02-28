package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.aicodegenerate.model.ModificationPlanResult;
import com.dango.dangoaicodeapp.domain.codegen.port.ModificationPlanningGateway;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory.AiModificationPlannerServiceFactory;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.AiModificationPlannerService;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class ModificationPlanningGatewayImpl implements ModificationPlanningGateway {

    @Resource
    private AiModificationPlannerServiceFactory aiModificationPlannerServiceFactory;

    @Override
    public ModificationPlanResult plan(long appId, String planningRequest) {
        AiModificationPlannerService service = aiModificationPlannerServiceFactory.createPlannerService(appId);
        return service.plan(appId, planningRequest);
    }

    @Override
    public TokenStream planStream(long appId, String planningRequest) {
        AiModificationPlannerService service = aiModificationPlannerServiceFactory.createPlannerService(appId);
        return service.planStream(appId, planningRequest);
    }
}
