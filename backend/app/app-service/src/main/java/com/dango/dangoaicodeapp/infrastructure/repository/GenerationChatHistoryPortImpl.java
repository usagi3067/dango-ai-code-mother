package com.dango.dangoaicodeapp.infrastructure.repository;

import com.dango.dangoaicodeapp.domain.app.entity.ChatHistory;
import com.dango.dangoaicodeapp.domain.app.repository.ChatHistoryRepository;
import com.dango.dangoaicodeapp.domain.codegen.model.GenerationTaskSnapshot;
import com.dango.dangoaicodeapp.domain.codegen.port.GenerationChatHistoryPort;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 聊天记录端口适配器。
 */
@Slf4j
@Component
public class GenerationChatHistoryPortImpl implements GenerationChatHistoryPort {

    @Resource
    private ChatHistoryRepository chatHistoryRepository;

    @Override
    public Long createGeneratingAiMessage(Long appId, Long userId) {
        // 占位消息由适配层统一落库，领域层只依赖“创建成功并返回 ID”语义。
        ChatHistory chatHistory = ChatHistory.createAiMessage(appId, userId, "", GenerationTaskSnapshot.STATUS_GENERATING);
        chatHistoryRepository.save(chatHistory);
        return chatHistory.getId();
    }

    @Override
    public void markAiMessageCompleted(Long chatHistoryId, String content) {
        updateAiMessage(chatHistoryId, content, GenerationTaskSnapshot.STATUS_COMPLETED);
    }

    @Override
    public void markAiMessageError(Long chatHistoryId, String content) {
        updateAiMessage(chatHistoryId, content, GenerationTaskSnapshot.STATUS_ERROR);
    }

    private void updateAiMessage(Long chatHistoryId, String content, String status) {
        // 状态与内容在同一处更新，避免 completed/error 分支实现漂移。
        ChatHistory chatHistory = chatHistoryRepository.findById(chatHistoryId).orElse(null);
        if (chatHistory == null) {
            log.warn("更新 AI 消息失败，记录不存在: {}", chatHistoryId);
            return;
        }
        chatHistory.setMessage(content == null ? "" : content);
        chatHistory.setStatus(status);
        chatHistory.setUpdateTime(LocalDateTime.now());
        chatHistoryRepository.updateById(chatHistory);
    }
}
