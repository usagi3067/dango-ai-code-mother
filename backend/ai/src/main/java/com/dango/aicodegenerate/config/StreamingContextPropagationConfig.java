package com.dango.aicodegenerate.config;

import com.dango.dangoaicodecommon.monitor.MonitorContext;
import com.dango.dangoaicodecommon.monitor.MonitorContextHolder;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 流式请求上下文传播配置
 *
 * 提供上下文传播的线程池，确保 MonitorContext 和 MDC 在流式响应线程中可用
 * 解决 LangChain4j SpringRestClient 默认线程池导致的上下文丢失问题
 */
@Configuration
public class StreamingContextPropagationConfig {

    /**
     * 上下文传播的流式请求线程池
     * 自动传播 MonitorContext + MDC 到线程池线程
     */
    @Bean("streamingContextPropagatingExecutor")
    AsyncTaskExecutor streamingContextPropagatingExecutor() {
        ThreadPoolTaskExecutor delegate = new ThreadPoolTaskExecutor();
        delegate.setThreadNamePrefix("LangChain4j-Stream-");
        delegate.setCorePoolSize(50);
        delegate.setMaxPoolSize(100);
        delegate.setQueueCapacity(200);
        delegate.setKeepAliveSeconds(60);
        delegate.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        delegate.initialize();
        return new ContextPropagatingTaskExecutor(delegate);
    }

    /**
     * 上下文传播的 AsyncTaskExecutor 包装器
     * 在任务提交时捕获 MonitorContext 和 MDC，在任务执行时恢复
     */
    static class ContextPropagatingTaskExecutor implements AsyncTaskExecutor {

        private final AsyncTaskExecutor delegate;

        ContextPropagatingTaskExecutor(AsyncTaskExecutor delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(Runnable task) {
            delegate.execute(wrap(task));
        }

        @Override
        public Future<?> submit(Runnable task) {
            return delegate.submit(wrap(task));
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return delegate.submit(wrapCallable(task));
        }

        private Runnable wrap(Runnable task) {
            MonitorContext monitorCtx = MonitorContextHolder.getContext();
            Map<String, String> mdcCtx = MDC.getCopyOfContextMap();
            return () -> {
                MonitorContext prevMonitor = MonitorContextHolder.getContext();
                Map<String, String> prevMdc = MDC.getCopyOfContextMap();
                try {
                    if (monitorCtx != null) {
                        MonitorContextHolder.setContext(monitorCtx);
                    }
                    if (mdcCtx != null) {
                        MDC.setContextMap(mdcCtx);
                    }
                    task.run();
                } finally {
                    if (prevMonitor != null) {
                        MonitorContextHolder.setContext(prevMonitor);
                    } else {
                        MonitorContextHolder.clearContext();
                    }
                    if (prevMdc != null) {
                        MDC.setContextMap(prevMdc);
                    } else {
                        MDC.clear();
                    }
                }
            };
        }

        private <T> Callable<T> wrapCallable(Callable<T> task) {
            MonitorContext monitorCtx = MonitorContextHolder.getContext();
            Map<String, String> mdcCtx = MDC.getCopyOfContextMap();
            return () -> {
                MonitorContext prevMonitor = MonitorContextHolder.getContext();
                Map<String, String> prevMdc = MDC.getCopyOfContextMap();
                try {
                    if (monitorCtx != null) {
                        MonitorContextHolder.setContext(monitorCtx);
                    }
                    if (mdcCtx != null) {
                        MDC.setContextMap(mdcCtx);
                    }
                    return task.call();
                } finally {
                    if (prevMonitor != null) {
                        MonitorContextHolder.setContext(prevMonitor);
                    } else {
                        MonitorContextHolder.clearContext();
                    }
                    if (prevMdc != null) {
                        MDC.setContextMap(prevMdc);
                    } else {
                        MDC.clear();
                    }
                }
            };
        }
    }
}