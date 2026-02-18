package com.dango.dangoaicodeapp.domain.app.entity;

import com.dango.dangoaicodecommon.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatHistoryTest {

    @Nested
    @DisplayName("createUserMessage - 创建用户消息")
    class CreateUserMessage {
        @Test
        void 正常创建() {
            ChatHistory msg = ChatHistory.createUserMessage(1L, 100L, "你好");
            assertEquals(1L, msg.getAppId());
            assertEquals(100L, msg.getUserId());
            assertEquals("你好", msg.getMessage());
            assertEquals("user", msg.getMessageType());
        }

        @Test
        void appId为空时抛异常() {
            assertThrows(BusinessException.class,
                    () -> ChatHistory.createUserMessage(null, 100L, "你好"));
        }

        @Test
        void message为空时抛异常() {
            assertThrows(BusinessException.class,
                    () -> ChatHistory.createUserMessage(1L, 100L, ""));
        }
    }

    @Nested
    @DisplayName("createAiMessage - 创建AI消息")
    class CreateAiMessage {
        @Test
        void 正常创建() {
            ChatHistory msg = ChatHistory.createAiMessage(1L, 100L, "你好，有什么可以帮你的？");
            assertEquals("ai", msg.getMessageType());
        }
    }

    @Nested
    @DisplayName("isUserMessage / isAiMessage - 类型判断")
    class MessageType {
        @Test
        void 用户消息判断() {
            ChatHistory msg = ChatHistory.createUserMessage(1L, 100L, "测试");
            assertTrue(msg.isUserMessage());
            assertFalse(msg.isAiMessage());
        }

        @Test
        void AI消息判断() {
            ChatHistory msg = ChatHistory.createAiMessage(1L, 100L, "测试");
            assertFalse(msg.isUserMessage());
            assertTrue(msg.isAiMessage());
        }
    }

    @Nested
    @DisplayName("validateMessage - 参数校验")
    class ValidateMessage {
        @Test
        void userId为空时抛异常() {
            assertThrows(BusinessException.class,
                    () -> ChatHistory.validateMessage(1L, null, "消息"));
        }

        @Test
        void 全部合法时不抛异常() {
            assertDoesNotThrow(
                    () -> ChatHistory.validateMessage(1L, 100L, "消息"));
        }
    }
}
