package com.dango.aicodegenerate.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.community.store.memory.chat.redis.StoreType;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;

import java.lang.reflect.Field;

@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
@Slf4j
public class RedisChatMemoryStoreConfig {

    private String host;

    private int port;

    private String password;

    private long ttl;

    private String username;

    /**
     * Redis 连接超时时间（毫秒），默认 10 秒
     */
    private int timeout = 10000;

    /**
     * Redis socket 读写超时时间（毫秒），默认 30 秒
     * 用于处理大数据量的读写操作，如保存大量聊天历史
     */
    private int socketTimeout = 30000;

    /**
     * 连接池最大连接数
     */
    private int maxTotal = 8;

    /**
     * 连接池最大空闲连接数
     */
    private int maxIdle = 8;

    /**
     * 连接池最小空闲连接数
     */
    private int minIdle = 0;

    @Bean
    public ChatMemoryStore redisChatMemoryStore() {
        // 创建带超时配置的 Jedis 客户端配置
        DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .timeoutMillis(timeout)
                .socketTimeoutMillis(socketTimeout)
                .user(username)
                .password(password)
                .build();

        // 配置连接池
        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        // 创建带连接池的 Jedis 客户端
        HostAndPort hostAndPort = new HostAndPort(host, port);
        JedisPooled jedis = new JedisPooled(poolConfig, hostAndPort, clientConfig);

        log.info("创建 RedisChatMemoryStore: host={}:{}, timeout={}ms, socketTimeout={}ms, maxTotal={}, maxIdle={}, minIdle={}",
                host, port, timeout, socketTimeout, maxTotal, maxIdle, minIdle);

        // 创建 RedisChatMemoryStore 并通过反射注入自定义 Jedis 客户端
        RedisChatMemoryStore store = RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .user(username)
                .password(password)
                .ttl(ttl)
                .storeType(StoreType.STRING)
                .build();

        try {
            // 通过反射替换内部的 Jedis 客户端
            Field clientField = RedisChatMemoryStore.class.getDeclaredField("client");
            clientField.setAccessible(true);

            // 直接设置新的客户端（不关闭旧客户端，避免影响连接池）
            clientField.set(store, jedis);
            log.info("成功替换 RedisChatMemoryStore 的 Jedis 客户端，使用自定义超时配置和连接池");
        } catch (Exception e) {
            log.error("替换 Jedis 客户端失败，将使用默认配置", e);
            // 如果反射失败，关闭我们创建的客户端，使用默认的
            jedis.close();
        }

        return store;
    }
}
