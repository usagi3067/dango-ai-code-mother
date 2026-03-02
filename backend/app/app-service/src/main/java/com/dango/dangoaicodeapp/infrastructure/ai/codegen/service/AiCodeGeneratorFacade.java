package com.dango.dangoaicodeapp.infrastructure.ai.codegen.service;

import com.dango.aicodegenerate.streaming.StreamingResponseProcessor;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.codegen.port.CodeGenerationPort;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private CodeGenerationPort codeGenerationPort;

    @Resource
    private StreamingResponseProcessor streamingProcessor;

    /**
     * 统一入口：生成并保存代码（流式, 使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 id
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        TokenStream tokenStream = codeGenerationPort.generateCodeStream(appId, codeGenTypeEnum, userMessage);
        // 使用 ai 模块的 StreamingResponseProcessor 处理流式响应
        return streamingProcessor.processAsJson(tokenStream);
    }
}
