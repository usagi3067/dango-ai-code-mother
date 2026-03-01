package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.aicodegenerate.model.ModificationPlanResult;
import com.dango.dangoaicodeapp.domain.codegen.port.ModificationPlanningGateway;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory.AiModificationPlannerServiceFactory;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.AiModificationPlannerService;
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
}
