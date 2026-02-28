package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.aicodegenerate.model.ImageCollectionPlan;
import com.dango.dangoaicodeapp.domain.codegen.port.ImageCollectionGateway;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.ImageCollectionPlanService;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.ImageCollectionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class ImageCollectionGatewayImpl implements ImageCollectionGateway {

    @Resource
    private ImageCollectionPlanService imageCollectionPlanService;

    @Resource
    private ImageCollectionService imageCollectionService;

    @Override
    public ImageCollectionPlan planImageCollection(String userPrompt) {
        return imageCollectionPlanService.planImageCollection(userPrompt);
    }

    @Override
    public String collectImages(String userPrompt) {
        return imageCollectionService.collectImages(userPrompt);
    }
}
