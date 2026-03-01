package com.dango.dangoaicodeapp.domain.codegen.service;

import cn.hutool.core.util.StrUtil;
import com.dango.dangoaicodeapp.domain.codegen.model.GenerationSession;
import com.dango.dangoaicodeapp.domain.codegen.model.GenerationStreamChunk;
import com.dango.dangoaicodeapp.domain.codegen.model.GenerationTaskSnapshot;
import com.dango.dangoaicodeapp.domain.codegen.port.GenerationChatHistoryPort;
import com.dango.dangoaicodeapp.domain.codegen.port.GenerationStreamPort;
import com.dango.dangoaicodeapp.domain.codegen.port.GenerationTaskPort;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 生成会话领域服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationSessionDomainServiceImpl implements GenerationSessionDomainService {

    private static final String STARTUP_COMPENSATION_MESSAGE = "任务启动失败，请重试";

    private final GenerationTaskPort generationTaskPort;
    private final GenerationChatHistoryPort generationChatHistoryPort;
    private final GenerationStreamPort generationStreamPort;

    @Override
    public GenerationSession startSession(Long appId, Long userId) {
        // 先预占任务，再创建占位消息：避免并发拒绝后留下孤儿 generating。
        if (!generationTaskPort.tryReserveTask(appId, userId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已有生成任务在运行中");
        }

        Long chatHistoryId = null;
        try {
            chatHistoryId = generationChatHistoryPort.createGeneratingAiMessage(appId, userId);
            generationTaskPort.bindChatHistoryId(appId, userId, chatHistoryId);
            String streamKey = generationTaskPort.getStreamKey(appId, userId);
            return new GenerationSession(appId, userId, chatHistoryId, streamKey);
        } catch (Exception e) {
            // 启动链路任一环节失败都做补偿，保证“任务锁/占位消息”不悬挂。
            compensateStartupFailure(appId, userId, chatHistoryId, e);
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "启动生成任务失败，请稍后重试");
        }
    }

    @Override
    public GenerationTaskSnapshot getTaskSnapshot(Long appId, Long userId) {
        return generationTaskPort.getTaskSnapshot(appId, userId);
    }

    @Override
    public void appendChunk(GenerationSession session, String content, String msgType) {
        generationStreamPort.appendChunk(session.streamKey(), content, msgType);
    }

    @Override
    public void completeSession(GenerationSession session, String finalAiContent) {
        generationTaskPort.markCompleted(session.appId(), session.userId());
        generationChatHistoryPort.markAiMessageCompleted(session.chatHistoryId(), finalAiContent);
    }

    @Override
    public void failSession(GenerationSession session, String errorContent) {
        generationTaskPort.markError(session.appId(), session.userId());
        generationChatHistoryPort.markAiMessageError(session.chatHistoryId(), errorContent);
    }

    @Override
    public void failSession(GenerationSession session, Throwable throwable) {
        // 错误文案策略收敛到领域层，避免应用层重复拼接/兜底。
        String message = throwable == null ? null : throwable.getMessage();
        String fallback = "AI回复失败，请稍后重试";
        failSession(session, StrUtil.isBlank(message) ? fallback : "AI回复失败: " + message);
    }

    @Override
    public List<GenerationStreamChunk> readStreamChunks(Long appId, Long userId, String afterId, long count) {
        String streamKey = generationTaskPort.getStreamKey(appId, userId);
        return generationStreamPort.readChunks(streamKey, afterId, count);
    }

    private void compensateStartupFailure(Long appId, Long userId, Long chatHistoryId, Exception rootCause) {
        log.error("生成会话启动失败，执行补偿: appId={}, userId={}, chatHistoryId={}",
                appId, userId, chatHistoryId, rootCause);

        try {
            generationTaskPort.cleanupTask(appId, userId);
        } catch (Exception cleanupEx) {
            log.error("清理任务锁失败: appId={}, userId={}", appId, userId, cleanupEx);
        }

        if (chatHistoryId == null) {
            return;
        }

        try {
            generationChatHistoryPort.markAiMessageError(chatHistoryId, STARTUP_COMPENSATION_MESSAGE);
        } catch (Exception messageEx) {
            log.error("补偿 AI 占位消息失败: chatHistoryId={}", chatHistoryId, messageEx);
        }
    }
}
