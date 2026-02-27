package com.dango.aicodegenerate.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

public interface AiModelProvider {
    ChatModel getChatModel(AiServiceType serviceType);
    StreamingChatModel getStreamingChatModel(AiServiceType serviceType);
}
