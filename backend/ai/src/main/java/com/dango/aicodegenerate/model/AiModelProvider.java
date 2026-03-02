package com.dango.aicodegenerate.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * AI 模型提供者接口
 *
 * <h2>功能说明</h2>
 * 提供获取 AI 模型实例的能力，支持同步和流式两种模式。
 *
 * <h2>设计理念</h2>
 * <ul>
 *   <li>使用字符串 serviceKey 而不是业务枚举，保持技术层的通用性</li>
 *   <li>业务模块可以自定义 serviceKey（如 "code-generator"、"user-chat"）</li>
 *   <li>配置文件中的 key 可以自由定义</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Resource
 * private AiModelProvider aiModelProvider;
 *
 * // 获取代码生成服务的流式模型
 * StreamingChatModel model = aiModelProvider.getStreamingChatModel("code-generator");
 *
 * // 获取用户对话服务的同步模型
 * ChatModel chatModel = aiModelProvider.getChatModel("user-chat");
 * }</pre>
 *
 * <h2>配置示例</h2>
 * <pre>
 * ai:
 *   services:
 *     code-generator:
 *       model: gpt-4-turbo
 *       max-tokens: 4096
 *     user-chat:
 *       model: gpt-3.5-turbo
 *       max-tokens: 2048
 * </pre>
 */
public interface AiModelProvider {

    /**
     * 获取同步聊天模型
     *
     * @param serviceKey 服务标识（如 "code-generator"、"user-chat"）
     * @return ChatModel 实例
     */
    ChatModel getChatModel(String serviceKey);

    /**
     * 获取流式聊天模型
     *
     * @param serviceKey 服务标识（如 "code-generator"、"user-chat"）
     * @return StreamingChatModel 实例
     */
    StreamingChatModel getStreamingChatModel(String serviceKey);
}
