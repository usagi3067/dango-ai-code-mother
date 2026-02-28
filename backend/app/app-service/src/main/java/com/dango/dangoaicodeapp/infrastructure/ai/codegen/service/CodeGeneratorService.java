package com.dango.dangoaicodeapp.infrastructure.ai.codegen.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface CodeGeneratorService {
    TokenStream generateCodeStream(@MemoryId long appId, @UserMessage String userMessage);
}
