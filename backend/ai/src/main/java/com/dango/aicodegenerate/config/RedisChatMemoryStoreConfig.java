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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;

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
        // 每30秒检查一次空闲连接，驱逐空闲超过60秒的连接
        // 防止长时间LLM调用（可能超过4分钟）后连接被云防火墙/NAT静默关闭
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setMinEvictableIdleDuration(Duration.ofSeconds(60));
        poolConfig.setNumTestsPerEvictionRun(3);

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
            // 第1步：取出 builder 创建的默认客户端并关闭（释放资源）
            // RedisChatMemoryStore.client 字段本身不参与实际 Redis 操作，仅在构造时
            // 传给 StringRedisOperations，之后闲置。此处只需取出来关闭，无需替换。
            Field clientField = RedisChatMemoryStore.class.getDeclaredField("client");
            clientField.setAccessible(true);
            UnifiedJedis oldClient = (UnifiedJedis) clientField.get(store);
            oldClient.close();

            // 第2步：替换 redisOperations（StringRedisOperations）
            // 所有实际 Redis 操作（getMessages/updateMessages/deleteMessages）均通过
            // StringRedisOperations.client 执行，而非 RedisChatMemoryStore.client。
            // StringRedisOperations 在 RedisChatMemoryStore 构造时就持有了原始客户端的副本，
            // 必须整体替换 redisOperations 实例，自定义连接池和超时配置才能真正生效。
            Class<?> strOpsClass = Class.forName(
                    "dev.langchain4j.community.store.memory.chat.redis.StringRedisOperations");
            Constructor<?> ctor = strOpsClass.getDeclaredConstructor(UnifiedJedis.class);
            ctor.setAccessible(true);
            Object newRedisOps = ctor.newInstance(jedis);

            Field redisOpsField = RedisChatMemoryStore.class.getDeclaredField("redisOperations");
            redisOpsField.setAccessible(true);
            redisOpsField.set(store, newRedisOps);

            log.info("成功替换 RedisChatMemoryStore 的 StringRedisOperations，使用自定义超时配置和连接池");
        } catch (Exception e) {
            log.error("替换 StringRedisOperations 失败，将使用默认配置（可能在长时间LLM调用后出现连接超时）", e);
            jedis.close();
        }

        return store;
    }
}
