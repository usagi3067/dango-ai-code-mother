package com.dango.dangoaicodeapp.domain.codegen.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface InterviewAnimationAdvisorService {
    @SystemMessage(fromResource = "prompt/interview-animation-advisor-prompt.txt")
    TokenStream advise(@UserMessage String userMessage);
}
