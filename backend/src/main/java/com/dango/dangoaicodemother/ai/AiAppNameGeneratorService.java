package com.dango.dangoaicodemother.ai;

import dev.langchain4j.service.SystemMessage;

/**
 * AI 应用名称生成服务接口
 * 使用 LangChain4j 框架，通过 AI 根据用户描述生成简洁的应用名称
 */
public interface AiAppNameGeneratorService {

    /**
     * 根据用户描述生成应用名称
     *
     * @param userDescription 用户的应用描述
     * @return 生成的应用名称（纯文本）
     */
    @SystemMessage(fromResource = "prompt/app-name-generator-system-prompt.txt")
    String generateAppName(String userDescription);
}
