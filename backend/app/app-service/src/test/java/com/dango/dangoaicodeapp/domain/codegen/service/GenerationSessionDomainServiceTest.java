package com.dango.dangoaicodeapp.domain.codegen.service;

import com.dango.dangoaicodeapp.domain.codegen.model.GenerationSession;
import com.dango.dangoaicodeapp.domain.codegen.model.GenerationStreamChunk;
import com.dango.dangoaicodeapp.domain.codegen.model.GenerationTaskSnapshot;
import com.dango.dangoaicodeapp.domain.codegen.port.GenerationChatHistoryPort;
import com.dango.dangoaicodeapp.domain.codegen.port.GenerationStreamPort;
import com.dango.dangoaicodeapp.domain.codegen.port.GenerationTaskPort;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationSessionDomainServiceTest {

    @Mock
    private GenerationTaskPort generationTaskPort;

    @Mock
    private GenerationChatHistoryPort generationChatHistoryPort;

    @Mock
    private GenerationStreamPort generationStreamPort;

    private GenerationSessionDomainServiceImpl domainService;

    @BeforeEach
    void setUp() {
        domainService = new GenerationSessionDomainServiceImpl(
                generationTaskPort,
                generationChatHistoryPort,
                generationStreamPort
        );
    }

    @Test
    void startSessionShouldReserveAndBindChatHistory() {
        when(generationTaskPort.tryReserveTask(1L, 2L)).thenReturn(true);
        when(generationChatHistoryPort.createGeneratingAiMessage(1L, 2L)).thenReturn(99L);
        when(generationTaskPort.getStreamKey(1L, 2L)).thenReturn("gen:stream:1:2");

        GenerationSession session = domainService.startSession(1L, 2L);

        assertEquals(1L, session.appId());
        assertEquals(2L, session.userId());
        assertEquals(99L, session.chatHistoryId());
        assertEquals("gen:stream:1:2", session.streamKey());

        InOrder inOrder = inOrder(generationTaskPort, generationChatHistoryPort);
        inOrder.verify(generationTaskPort).tryReserveTask(1L, 2L);
        inOrder.verify(generationChatHistoryPort).createGeneratingAiMessage(1L, 2L);
        inOrder.verify(generationTaskPort).bindChatHistoryId(1L, 2L, 99L);
    }

    @Test
    void startSessionShouldRejectWhenTaskAlreadyRunning() {
        when(generationTaskPort.tryReserveTask(1L, 2L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> domainService.startSession(1L, 2L));

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), ex.getCode());
        verify(generationChatHistoryPort, never()).createGeneratingAiMessage(1L, 2L);
    }

    @Test
    void startSessionShouldCompensateWhenBindFails() {
        when(generationTaskPort.tryReserveTask(1L, 2L)).thenReturn(true);
        when(generationChatHistoryPort.createGeneratingAiMessage(1L, 2L)).thenReturn(100L);
        org.mockito.Mockito.doThrow(new RuntimeException("redis unavailable"))
                .when(generationTaskPort)
                .bindChatHistoryId(1L, 2L, 100L);

        BusinessException ex = assertThrows(BusinessException.class, () -> domainService.startSession(1L, 2L));

        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), ex.getCode());
        verify(generationTaskPort).cleanupTask(1L, 2L);
        verify(generationChatHistoryPort).markAiMessageError(eq(100L), contains("任务启动失败"));
    }

    @Test
    void readStreamChunksShouldUseStreamKeyFromTaskPort() {
        when(generationTaskPort.getStreamKey(1L, 2L)).thenReturn("gen:stream:1:2");
        List<GenerationStreamChunk> expected = List.of(new GenerationStreamChunk("1-0", "chunk", "token"));
        when(generationStreamPort.readChunks("gen:stream:1:2", "0", 100)).thenReturn(expected);

        List<GenerationStreamChunk> actual = domainService.readStreamChunks(1L, 2L, "0", 100);

        assertEquals(expected, actual);
    }

    @Test
    void completeSessionShouldSyncTaskAndChatHistory() {
        GenerationSession session = new GenerationSession(1L, 2L, 3L, "gen:stream:1:2");

        domainService.completeSession(session, "done");

        verify(generationTaskPort).markCompleted(1L, 2L);
        verify(generationChatHistoryPort).markAiMessageCompleted(3L, "done");
    }

    @Test
    void getTaskSnapshotShouldDelegateToPort() {
        GenerationTaskSnapshot snapshot = new GenerationTaskSnapshot(GenerationTaskSnapshot.STATUS_GENERATING, 11L);
        when(generationTaskPort.getTaskSnapshot(1L, 2L)).thenReturn(snapshot);

        GenerationTaskSnapshot actual = domainService.getTaskSnapshot(1L, 2L);

        assertEquals(snapshot, actual);
    }

    @Test
    void failSessionWithThrowableShouldFallbackMessage() {
        GenerationSession session = new GenerationSession(1L, 2L, 3L, "gen:stream:1:2");

        domainService.failSession(session, new RuntimeException());

        verify(generationTaskPort).markError(1L, 2L);
        verify(generationChatHistoryPort).markAiMessageError(3L, "AI回复失败，请稍后重试");
    }
}
