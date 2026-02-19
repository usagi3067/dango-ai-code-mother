package com.dango.aicodegenerate.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 支持自定义 JedisPooled（含超时、连接池、重试配置）的 ChatMemoryStore 实现。
 * 替代 langchain4j 内置的 RedisChatMemoryStore（不支持超时配置）。
 */
public class JedisBackedChatMemoryStore implements ChatMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(JedisBackedChatMemoryStore.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 500;

    private final JedisPooled client;
    private final long ttl;

    public JedisBackedChatMemoryStore(JedisPooled client, long ttl) {
        this.client = client;
        this.ttl = ttl;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = executeWithRetry(() -> client.get(toKey(memoryId)));
        return json == null ? new ArrayList<>() : ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = ChatMessageSerializer.messagesToJson(messages);
        String key = toKey(memoryId);
        if (ttl > 0) {
            executeWithRetry(() -> client.setex(key, ttl, json));
        } else {
            executeWithRetry(() -> client.set(key, json));
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        executeWithRetry(() -> client.del(toKey(memoryId)));
    }

    private <T> T executeWithRetry(Supplier<T> action) {
        JedisConnectionException lastException = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                return action.get();
            } catch (JedisConnectionException e) {
                lastException = e;
                log.warn("Redis 操作失败，第 {} 次重试: {}", i + 1, e.getMessage());
                if (i < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * (i + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        throw lastException;
    }

    private String toKey(Object memoryId) {
        return memoryId.toString();
    }
}
