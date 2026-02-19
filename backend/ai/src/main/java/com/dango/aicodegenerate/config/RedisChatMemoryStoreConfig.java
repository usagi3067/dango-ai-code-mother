package com.dango.aicodegenerate.config;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.Data;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {

    private String host;

    private int port;

    private String password;

    private long ttl;

    private String username;

    @Bean
    public ChatMemoryStore redisChatMemoryStore() {
        DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .user(username)
                .password(password)
                .connectionTimeoutMillis(5000)
                .socketTimeoutMillis(5000)
                .build();

        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);

        JedisPooled jedis = new JedisPooled(
                poolConfig, new HostAndPort(host, port), clientConfig);

        return new JedisBackedChatMemoryStore(jedis, ttl);
    }
}
