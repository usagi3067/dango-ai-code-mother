package com.dango.dangoaicodeapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * OpenAI 流式聊天模型线程池配置
 * 
 * 解决 LangChain4j 默认线程池只有 1 个核心线程导致的并发阻塞问题
 * 
 * AI 调用属于 IO 密集型任务，大部分时间在等待网络响应，可以配置较多线程
 */
@Configuration
public class OpenAiStreamChatModelConfig {

    @Bean("openAiStreamingChatModelTaskExecutor")
    AsyncTaskExecutor openAiStreamingChatModelTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("LangChain4j-OpenAI-");
        // IO 密集型任务，核心线程数可以设置较大
        taskExecutor.setCorePoolSize(50);
        // 最大线程数，应对突发流量
        taskExecutor.setMaxPoolSize(100);
        // 队列容量，避免无限堆积
        taskExecutor.setQueueCapacity(200);
        // 线程空闲时间（秒），超过核心线程数的线程在空闲后会被回收
        taskExecutor.setKeepAliveSeconds(60);
        // 拒绝策略：由调用线程执行，避免任务丢失
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.initialize();
        return taskExecutor;
    }
}
