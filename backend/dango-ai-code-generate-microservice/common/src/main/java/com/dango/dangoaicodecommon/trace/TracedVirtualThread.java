package com.dango.dangoaicodecommon.trace;

import org.apache.skywalking.apm.toolkit.trace.CallableWrapper;
import org.apache.skywalking.apm.toolkit.trace.RunnableWrapper;
import org.apache.skywalking.apm.toolkit.trace.SupplierWrapper;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.slf4j.MDC;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * 支持 SkyWalking 完整链路追踪的虚拟线程工具
 * <p>
 * 功能：
 * 1. 使用 SkyWalking 官方 Wrapper 实现跨线程 Span 上下文传递（完整链路追踪）
 * 2. 同时传递 traceId 到 MDC，确保日志中显示 traceId
 * <p>
 * 使用示例：
 * <pre>
 * // 替换 Thread.startVirtualThread(() -> { ... });
 * TracedVirtualThread.start(() -> {
 *     log.info("这里的日志会包含 traceId，且在 SkyWalking UI 中可见完整链路");
 * });
 *
 * // 带返回值的异步任务
 * CompletableFuture<String> future = TracedVirtualThread.supplyAsync(() -> {
 *     return "result";
 * });
 * </pre>
 * <p>
 * 注意：完整链路追踪需要 SkyWalking Agent 运行时增强，Debug 模式下可能无法正常工作
 */
public class TracedVirtualThread {

    private static final String TRACE_ID_KEY = "tid";

    /**
     * 启动一个带完整 trace 传递的虚拟线程
     * <p>
     * 同时支持：
     * - SkyWalking Span 上下文传递（链路追踪）
     * - MDC traceId 传递（日志显示）
     *
     * @param task 要执行的任务
     * @return 虚拟线程实例
     */
    public static Thread start(Runnable task) {
        String traceId = captureTraceId();
        // 先用 SkyWalking Wrapper 包装（传递 Span 上下文），再包装 MDC
        Runnable skywalkingWrapped = RunnableWrapper.of(task);
        return Thread.startVirtualThread(wrapWithMdc(skywalkingWrapped, traceId));
    }

    /**
     * 启动一个带完整 trace 传递的虚拟线程（带名称）
     *
     * @param name 线程名称
     * @param task 要执行的任务
     * @return 虚拟线程实例
     */
    public static Thread start(String name, Runnable task) {
        String traceId = captureTraceId();
        Runnable skywalkingWrapped = RunnableWrapper.of(task);
        return Thread.ofVirtual()
                .name(name)
                .start(wrapWithMdc(skywalkingWrapped, traceId));
    }

    /**
     * 异步执行任务（无返回值）
     *
     * @param task 要执行的任务
     * @return CompletableFuture
     */
    public static CompletableFuture<Void> runAsync(Runnable task) {
        String traceId = captureTraceId();
        Runnable skywalkingWrapped = RunnableWrapper.of(task);
        return CompletableFuture.runAsync(wrapWithMdc(skywalkingWrapped, traceId), virtualThreadExecutor());
    }

    /**
     * 异步执行任务（有返回值）
     *
     * @param supplier 要执行的任务
     * @param <T>      返回值类型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        String traceId = captureTraceId();
        Supplier<T> skywalkingWrapped = SupplierWrapper.of(supplier);
        return CompletableFuture.supplyAsync(wrapWithMdc(skywalkingWrapped, traceId), virtualThreadExecutor());
    }

    /**
     * 包装 Callable，支持完整 trace 传递
     *
     * @param task 原始任务
     * @param <T>  返回值类型
     * @return 包装后的 Callable
     */
    public static <T> Callable<T> wrapCallable(Callable<T> task) {
        String traceId = captureTraceId();
        Callable<T> skywalkingWrapped = CallableWrapper.of(task);
        return wrapWithMdc(skywalkingWrapped, traceId);
    }

    /**
     * 包装 Runnable，支持完整 trace 传递
     *
     * @param task 原始任务
     * @return 包装后的 Runnable
     */
    public static Runnable wrapRunnable(Runnable task) {
        String traceId = captureTraceId();
        Runnable skywalkingWrapped = RunnableWrapper.of(task);
        return wrapWithMdc(skywalkingWrapped, traceId);
    }

    /**
     * 包装 Supplier，支持完整 trace 传递
     *
     * @param supplier 原始 Supplier
     * @param <T>      返回值类型
     * @return 包装后的 Supplier
     */
    public static <T> Supplier<T> wrapSupplier(Supplier<T> supplier) {
        String traceId = captureTraceId();
        Supplier<T> skywalkingWrapped = SupplierWrapper.of(supplier);
        return wrapWithMdc(skywalkingWrapped, traceId);
    }

    // ==================== MDC 包装方法 ====================

    /**
     * 包装 Runnable，添加 MDC traceId 传递
     */
    private static Runnable wrapWithMdc(Runnable task, String traceId) {
        return () -> {
            try {
                setTraceId(traceId);
                task.run();
            } finally {
                clearTraceId();
            }
        };
    }

    /**
     * 包装 Callable，添加 MDC traceId 传递
     */
    private static <T> Callable<T> wrapWithMdc(Callable<T> task, String traceId) {
        return () -> {
            try {
                setTraceId(traceId);
                return task.call();
            } finally {
                clearTraceId();
            }
        };
    }

    /**
     * 包装 Supplier，添加 MDC traceId 传递
     */
    private static <T> Supplier<T> wrapWithMdc(Supplier<T> supplier, String traceId) {
        return () -> {
            try {
                setTraceId(traceId);
                return supplier.get();
            } finally {
                clearTraceId();
            }
        };
    }

    // ==================== 工具方法 ====================

    /**
     * 捕获当前线程的 traceId
     *
     * @return traceId，如果不存在则返回 null
     */
    public static String captureTraceId() {
        String traceId = TraceContext.traceId();
        if (traceId == null || traceId.isEmpty() || "N/A".equalsIgnoreCase(traceId)) {
            return null;
        }
        return traceId;
    }

    /**
     * 设置 traceId 到 MDC
     */
    private static void setTraceId(String traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 清除 MDC 中的 traceId
     */
    private static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * 获取虚拟线程执行器
     */
    private static Executor virtualThreadExecutor() {
        return task -> Thread.startVirtualThread(task);
    }
}
