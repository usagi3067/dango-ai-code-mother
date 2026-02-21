package com.dango.dangoaicodeapp.domain.codegen.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface IntentClassifierService {
    @SystemMessage(fromResource = "prompt/intent-classifier-prompt.txt")
    String classify(@UserMessage String userMessage);
}
