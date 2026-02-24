package com.dango.dangoaicodeapp.domain.codegen.ai.factory;

import com.dango.dangoaicodeapp.domain.codegen.ai.service.InterviewAnimationAdvisorService;
import com.dango.dangoaicodeapp.domain.codegen.ai.service.LeetCodeAnimationAdvisorService;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * LeetCode 动画设计建议 AI 服务工厂
 */
@Component
public class AiAnimationAdvisorServiceFactory {

    @Resource
    private StreamingChatModel streamingChatModel;

    public LeetCodeAnimationAdvisorService createService() {
        return AiServices.builder(LeetCodeAnimationAdvisorService.class)
                .streamingChatModel(streamingChatModel)
                .build();
    }

    public InterviewAnimationAdvisorService createInterviewService() {
        return AiServices.builder(InterviewAnimationAdvisorService.class)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}
