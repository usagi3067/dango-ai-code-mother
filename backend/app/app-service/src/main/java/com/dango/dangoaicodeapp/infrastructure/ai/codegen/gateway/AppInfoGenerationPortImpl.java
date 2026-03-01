package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.aicodegenerate.model.AppNameAndTagResult;
import com.dango.dangoaicodeapp.domain.codegen.port.AppInfoGenerationPort;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.AiAppInfoGeneratorService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class AppInfoGenerationPortImpl implements AppInfoGenerationPort {

    @Resource
    private AiAppInfoGeneratorService aiAppInfoGeneratorService;

    @Override
    public AppNameAndTagResult generateAppInfo(String userDescription) {
        return aiAppInfoGeneratorService.generateAppInfo(userDescription);
    }
}
