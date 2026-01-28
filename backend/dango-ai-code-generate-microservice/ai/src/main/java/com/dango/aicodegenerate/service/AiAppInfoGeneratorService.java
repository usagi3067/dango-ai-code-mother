package com.dango.aicodegenerate.service;


import com.dango.aicodegenerate.model.AppNameAndTagResult;
import dev.langchain4j.service.SystemMessage;

/**
 * AI 应用信息生成服务接口
 * 使用 LangChain4j 框架，通过 AI 根据用户描述生成应用名称和标签
 */
public interface AiAppInfoGeneratorService {

    /**
     * 根据用户描述生成应用名称和标签
     *
     * @param userDescription 用户的应用描述
     * @return 生成的应用名称和标签（结构化输出）
     */
    @SystemMessage(fromResource = "prompt/app-info-generator-system-prompt.txt")
    AppNameAndTagResult generateAppInfo(String userDescription);
}
