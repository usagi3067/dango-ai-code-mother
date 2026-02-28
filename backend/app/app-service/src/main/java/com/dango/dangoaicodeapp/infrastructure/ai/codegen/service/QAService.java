package com.dango.dangoaicodeapp.infrastructure.ai.codegen.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface QAService {
    @SystemMessage(fromResource = "prompt/qa-system-prompt.txt")
    TokenStream answer(@MemoryId long appId, @UserMessage String userMessage);
}
