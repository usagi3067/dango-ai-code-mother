package com.dango.dangoaicodemother.core.wrapper;

import cn.hutool.json.JSONUtil;
import com.dango.dangoaicodemother.ai.model.message.AiResponseMessage;
import reactor.core.publisher.Flux;

/**
 * 流式消息包装器
 * 将纯文本包装为统一的 JSON 格式消息
 */
public class StreamMessageWrapper {

    /**
     * 将纯文本包装为 AiResponseMessage JSON 格式
     *
     * @param text 原始文本
     * @return JSON 格式字符串 {"type": "ai_response", "data": "..."}
     */
    public static String wrapAsAiResponse(String text) {
        AiResponseMessage message = new AiResponseMessage(text);
        return JSONUtil.toJsonStr(message);
    }

    /**
     * 将 Flux<String> 流包装为 JSON 格式流
     *
     * @param textFlux 原始文本流
     * @return 包装后的 JSON 格式流
     */
    public static Flux<String> wrapFlux(Flux<String> textFlux) {
        return textFlux.map(StreamMessageWrapper::wrapAsAiResponse);
    }
}
