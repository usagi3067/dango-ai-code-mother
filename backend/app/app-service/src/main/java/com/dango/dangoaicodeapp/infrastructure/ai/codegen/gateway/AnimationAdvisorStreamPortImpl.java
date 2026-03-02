package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import com.dango.dangoaicodeapp.domain.codegen.port.AnimationAdvisorStreamPort;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory.AiAnimationAdvisorServiceFactory;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.InterviewAnimationAdvisorService;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.InterviewSourceCodeAdvisorService;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.LeetCodeAnimationAdvisorService;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 动画建议流端口适配器。
 */
@Component
@RequiredArgsConstructor
public class AnimationAdvisorStreamPortImpl implements AnimationAdvisorStreamPort {

    private final AiAnimationAdvisorServiceFactory aiAnimationAdvisorServiceFactory;
    private final TokenStreamMessageFluxFactory tokenStreamMessageFluxFactory;

    @Override
    public Flux<String> adviseLeetCode(String userPrompt) {
        LeetCodeAnimationAdvisorService service = aiAnimationAdvisorServiceFactory.createService();
        TokenStream tokenStream = service.advise(userPrompt);
        return tokenStreamMessageFluxFactory.toChunkFlux(tokenStream);
    }

    @Override
    public Flux<String> adviseInterview(String userPrompt) {
        InterviewAnimationAdvisorService service = aiAnimationAdvisorServiceFactory.createInterviewService();
        TokenStream tokenStream = service.advise(userPrompt);
        return tokenStreamMessageFluxFactory.toChunkFlux(tokenStream);
    }

    @Override
    public Flux<String> adviseInterviewSourceCode(String userPrompt) {
        InterviewSourceCodeAdvisorService service = aiAnimationAdvisorServiceFactory.createInterviewSourceCodeService();
        TokenStream tokenStream = service.advise(userPrompt);
        return tokenStreamMessageFluxFactory.toChunkFlux(tokenStream);
    }
}

