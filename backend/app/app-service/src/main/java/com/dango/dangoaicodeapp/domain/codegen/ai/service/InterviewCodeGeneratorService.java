package com.dango.dangoaicodeapp.domain.codegen.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface InterviewCodeGeneratorService extends CodeGeneratorService {
    @SystemMessage(fromResource = "prompt/codegen-interview-system-prompt.txt")
    @Override
    TokenStream generateCodeStream(@MemoryId long appId, @UserMessage String userMessage);
}
