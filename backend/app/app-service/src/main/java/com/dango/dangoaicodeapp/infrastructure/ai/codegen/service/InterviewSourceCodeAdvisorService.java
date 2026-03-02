package com.dango.dangoaicodeapp.infrastructure.ai.codegen.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface InterviewSourceCodeAdvisorService {
    @SystemMessage(fromResource = "prompt/interview-source-code-advisor-prompt.txt")
    TokenStream advise(@UserMessage String userMessage);
}
