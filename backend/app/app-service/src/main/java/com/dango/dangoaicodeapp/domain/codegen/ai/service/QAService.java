package com.dango.dangoaicodeapp.domain.codegen.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface QAService {
    @SystemMessage(fromResource = "prompt/qa-system-prompt.txt")
    TokenStream answer(@UserMessage String userMessage);
}
