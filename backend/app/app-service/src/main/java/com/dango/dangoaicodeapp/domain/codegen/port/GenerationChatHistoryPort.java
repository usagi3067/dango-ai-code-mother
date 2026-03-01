package com.dango.dangoaicodeapp.domain.codegen.port;

/**
 * 生成会话聊天记录端口。
 *
 * 任务状态与聊天占位消息分属不同存储，
 * 通过端口把二者的一致性规则托管给领域服务，避免编排层散落补偿逻辑。
 */
public interface GenerationChatHistoryPort {

    /**
     * 创建 AI 占位消息（generating）。
     * 与任务预占配合，形成“先有任务再有占位消息”的一致性规则。
     */
    Long createGeneratingAiMessage(Long appId, Long userId);

    /**
     * 标记 AI 消息完成并落最终内容。
     */
    void markAiMessageCompleted(Long chatHistoryId, String content);

    /**
     * 标记 AI 消息失败并落错误/部分内容。
     */
    void markAiMessageError(Long chatHistoryId, String content);
}
