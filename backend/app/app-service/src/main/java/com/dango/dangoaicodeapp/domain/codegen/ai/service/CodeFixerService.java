package com.dango.dangoaicodeapp.domain.codegen.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface CodeFixerService {
    TokenStream fixCodeStream(@MemoryId long appId, @UserMessage String fixRequest);
}
