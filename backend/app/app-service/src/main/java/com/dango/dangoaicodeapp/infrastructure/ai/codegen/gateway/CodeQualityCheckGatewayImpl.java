package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.aicodegenerate.model.QualityResult;
import com.dango.dangoaicodeapp.domain.codegen.port.CodeQualityCheckGateway;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.CodeQualityCheckService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CodeQualityCheckGatewayImpl implements CodeQualityCheckGateway {

    @Resource
    private CodeQualityCheckService codeQualityCheckService;

    @Override
    public QualityResult checkCodeQuality(String codeContent) {
        return codeQualityCheckService.checkCodeQuality(codeContent);
    }
}
