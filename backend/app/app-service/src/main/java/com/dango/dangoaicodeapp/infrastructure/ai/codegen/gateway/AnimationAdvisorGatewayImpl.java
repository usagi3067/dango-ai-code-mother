package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.dangoaicodeapp.domain.codegen.port.AnimationAdvisorGateway;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory.AiAnimationAdvisorServiceFactory;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.InterviewAnimationAdvisorService;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.LeetCodeAnimationAdvisorService;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class AnimationAdvisorGatewayImpl implements AnimationAdvisorGateway {

    @Resource
    private AiAnimationAdvisorServiceFactory aiAnimationAdvisorServiceFactory;

    @Override
    public TokenStream adviseLeetCode(String userPrompt) {
        LeetCodeAnimationAdvisorService service = aiAnimationAdvisorServiceFactory.createService();
        return service.advise(userPrompt);
    }

    @Override
    public TokenStream adviseInterview(String userPrompt) {
        InterviewAnimationAdvisorService service = aiAnimationAdvisorServiceFactory.createInterviewService();
        return service.advise(userPrompt);
    }
}
