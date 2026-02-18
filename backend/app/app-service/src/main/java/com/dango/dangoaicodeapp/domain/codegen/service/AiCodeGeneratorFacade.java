package com.dango.dangoaicodeapp.domain.codegen.service;

import cn.hutool.json.JSONUtil;

import com.dango.aicodegenerate.extractor.ToolArgumentsExtractor;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.aicodegenerate.model.message.StreamMessage;
import com.dango.aicodegenerate.model.message.ToolExecutedMessage;
import com.dango.dangoaicodeapp.domain.codegen.ai.service.AiCodeGeneratorService;
import com.dango.dangoaicodeapp.domain.codegen.ai.factory.AiCodeGeneratorServiceFactory;
import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 统一入口：生成并保存代码（流式, 使用 appId）
     * 统一使用 VUE_PROJECT 类型
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 id
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, CodeGenTypeEnum.VUE_PROJECT);
        TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
        return processTokenStream(tokenStream);
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            // 为每个工具调用维护一个 extractor
            Map<String, ToolArgumentsExtractor> extractors = new ConcurrentHashMap<>();

            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        String toolId = toolExecutionRequest.id();
                        String toolName = toolExecutionRequest.name();
                        String delta = toolExecutionRequest.arguments();

                        // 获取或创建 extractor
                        ToolArgumentsExtractor extractor = extractors.computeIfAbsent(
                            toolId,
                            id -> new ToolArgumentsExtractor(id, toolName)
                        );

                        // 处理 delta，获取需要发送的消息
                        List<StreamMessage> messages = extractor.process(delta);
                        for (StreamMessage msg : messages) {
                            sink.next(JSONUtil.toJsonStr(msg));
                        }
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }
}
