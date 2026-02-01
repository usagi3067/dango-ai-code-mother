package com.dango.dangoaicodeapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * OpenAI 流式聊天模型线程池配置
 * 
 * 解决 LangChain4j 默认线程池只有 1 个核心线程导致的并发阻塞问题
 */
@Configuration
public class OpenAiStreamChatModelConfig {

    @Bean("openAiStreamingChatModelTaskExecutor")
    AsyncTaskExecutor openAiStreamingChatModelTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("LangChain4j-OpenAI-");
        taskExecutor.setCorePoolSize(6);
        return taskExecutor;
    }
}
