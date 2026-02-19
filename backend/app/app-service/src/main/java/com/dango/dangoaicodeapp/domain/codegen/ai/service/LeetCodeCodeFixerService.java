package com.dango.dangoaicodeapp.domain.codegen.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface LeetCodeCodeFixerService extends CodeFixerService {
    @SystemMessage(fromResource = "prompt/codegen-fix-leetcode-system-prompt.txt")
    @Override
    TokenStream fixCodeStream(@MemoryId long appId, @UserMessage String fixRequest);
}
