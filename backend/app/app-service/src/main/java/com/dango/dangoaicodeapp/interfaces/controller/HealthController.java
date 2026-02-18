package com.dango.dangoaicodeapp.interfaces.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.apache.skywalking.apm.toolkit.trace.TraceCrossThread;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 健康检查控制器 - 用于验证 SkyWalking traceId 集成
 */
@Slf4j
@RestController
@RequestMapping("/health")
public class HealthController {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @GetMapping("/check")
    public Map<String, Object> healthCheck() {
        // 1. 主线程日志
        log.info("=== 健康检查开始（主线程）===");
        String traceId = TraceContext.traceId();
        log.info("主线程 traceId: {}", traceId);

        // 2. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("service", "app-service");
        result.put("status", "UP");
        result.put("traceId", traceId);
        result.put("traceIdValid", traceId != null && !traceId.isEmpty() && !"N/A".equals(traceId));

        log.info("=== 健康检查结束（主线程）===");
        return result;
    }

    /**
     * 测试异步线程中的 traceId 传递
     */
    @GetMapping("/async")
    public Map<String, Object> asyncCheck() throws Exception {
        log.info("=== 异步测试开始（主线程）===");
        String mainTraceId = TraceContext.traceId();
        log.info("主线程 traceId: {}", mainTraceId);

        // 使用 @TraceCrossThread 注解的 Callable
        Future<String> future = executor.submit(new TracedCallable());
        String asyncTraceId = future.get();

        Map<String, Object> result = new HashMap<>();
        result.put("service", "app-service");
        result.put("mainThreadTraceId", mainTraceId);
        result.put("asyncThreadTraceId", asyncTraceId);
        result.put("traceIdMatch", mainTraceId != null && mainTraceId.equals(asyncTraceId));

        log.info("=== 异步测试结束（主线程）===");
        return result;
    }

    /**
     * 使用 @TraceCrossThread 注解实现跨线程 trace 传递
     */
    @TraceCrossThread
    public static class TracedCallable implements Callable<String> {
        @Override
        public String call() {
            log.info("=== 异步线程执行 ===");
            String traceId = TraceContext.traceId();
            log.info("异步线程 traceId: {}", traceId);
            return traceId;
        }
    }
}
