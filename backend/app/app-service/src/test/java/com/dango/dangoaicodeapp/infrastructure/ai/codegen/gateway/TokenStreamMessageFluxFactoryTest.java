package com.dango.dangoaicodeapp.infrastructure.ai.codegen.gateway;

import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.aicodegenerate.model.message.StreamMessage;
import com.dango.aicodegenerate.model.message.StreamMessageTypeEnum;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TokenStreamMessageFluxFactoryTest {

    @Test
    @DisplayName("无 partial 文本时，complete 响应文本应回填为 ai_response")
    void shouldEmitCompleteResponseTextWhenNoPartialResponse() {
        TokenStreamMessageFluxFactory factory = new TokenStreamMessageFluxFactory();
        ChatResponse completeResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("final message"))
                .build();
        FakeTokenStream tokenStream = FakeTokenStream.withCompleteOnly(completeResponse);

        List<String> messages = factory.toTextAndToolFlux(tokenStream).collectList().block();

        assertEquals(1, messages.size());
        StreamMessage streamMessage = JSONUtil.toBean(messages.get(0), StreamMessage.class);
        assertEquals(StreamMessageTypeEnum.AI_RESPONSE.getValue(), streamMessage.getType());
        AiResponseMessage aiResponseMessage = JSONUtil.toBean(messages.get(0), AiResponseMessage.class);
        assertEquals("final message", aiResponseMessage.getData());
    }

    @Test
    @DisplayName("已有 partial 文本时，不应重复回填 complete 文本")
    void shouldNotDuplicateWhenPartialResponseAlreadyExists() {
        TokenStreamMessageFluxFactory factory = new TokenStreamMessageFluxFactory();
        ChatResponse completeResponse = ChatResponse.builder()
                .aiMessage(AiMessage.from("final message"))
                .build();
        FakeTokenStream tokenStream = FakeTokenStream.withPartialThenComplete("partial chunk", completeResponse);

        List<String> messages = factory.toTextAndToolFlux(tokenStream).collectList().block();

        assertEquals(1, messages.size());
        AiResponseMessage aiResponseMessage = JSONUtil.toBean(messages.get(0), AiResponseMessage.class);
        assertEquals("partial chunk", aiResponseMessage.getData());
        assertFalse(aiResponseMessage.getData().contains("final message"));
    }

    private static final class FakeTokenStream implements TokenStream {
        private Consumer<String> partialResponseConsumer;
        private Consumer<ToolExecution> toolExecutedConsumer;
        private Consumer<ChatResponse> completeResponseConsumer;
        private Consumer<Throwable> errorConsumer;
        private Runnable startAction;

        static FakeTokenStream withCompleteOnly(ChatResponse response) {
            FakeTokenStream stream = new FakeTokenStream();
            stream.startAction = () -> stream.completeResponseConsumer.accept(response);
            return stream;
        }

        static FakeTokenStream withPartialThenComplete(String partial, ChatResponse response) {
            FakeTokenStream stream = new FakeTokenStream();
            stream.startAction = () -> {
                stream.partialResponseConsumer.accept(partial);
                stream.completeResponseConsumer.accept(response);
            };
            return stream;
        }

        @Override
        public TokenStream onPartialResponse(Consumer<String> partialResponseConsumer) {
            this.partialResponseConsumer = partialResponseConsumer;
            return this;
        }

        @Override
        public TokenStream onRetrieved(Consumer<List<Content>> consumer) {
            return this;
        }

        @Override
        public TokenStream onToolExecuted(Consumer<ToolExecution> toolExecutionConsumer) {
            this.toolExecutedConsumer = toolExecutionConsumer;
            return this;
        }

        @Override
        public TokenStream onCompleteToolExecutionRequest(BiConsumer<Integer, ToolExecutionRequest> consumer) {
            return this;
        }

        @Override
        public TokenStream onPartialToolExecutionRequest(BiConsumer<Integer, ToolExecutionRequest> consumer) {
            return this;
        }

        @Override
        public TokenStream onCompleteResponse(Consumer<ChatResponse> completeResponseConsumer) {
            this.completeResponseConsumer = completeResponseConsumer;
            return this;
        }

        @Override
        public TokenStream onError(Consumer<Throwable> errorHandler) {
            this.errorConsumer = errorHandler;
            return this;
        }

        @Override
        public TokenStream ignoreErrors() {
            return this;
        }

        @Override
        public void start() {
            if (startAction != null) {
                startAction.run();
            }
        }
    }
}
