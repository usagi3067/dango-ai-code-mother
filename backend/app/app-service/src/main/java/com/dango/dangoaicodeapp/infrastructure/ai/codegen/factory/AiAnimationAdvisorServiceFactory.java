package com.dango.dangoaicodeapp.infrastructure.ai.codegen.factory;

import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.InterviewAnimationAdvisorService;
import com.dango.dangoaicodeapp.infrastructure.ai.codegen.service.LeetCodeAnimationAdvisorService;
import com.dango.aicodegenerate.model.AiModelProvider;
import com.dango.aicodegenerate.model.AiServiceType;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * LeetCode 动画设计建议 AI 服务工厂
 */
@Component
public class AiAnimationAdvisorServiceFactory {

    @Resource
    private AiModelProvider aiModelProvider;

    public LeetCodeAnimationAdvisorService createService() {
        return AiServices.builder(LeetCodeAnimationAdvisorService.class)
                .streamingChatModel(aiModelProvider.getStreamingChatModel(AiServiceType.ANIMATION_ADVISOR))
                .build();
    }

    public InterviewAnimationAdvisorService createInterviewService() {
        return AiServices.builder(InterviewAnimationAdvisorService.class)
                .streamingChatModel(aiModelProvider.getStreamingChatModel(AiServiceType.ANIMATION_ADVISOR))
                .build();
    }
}
