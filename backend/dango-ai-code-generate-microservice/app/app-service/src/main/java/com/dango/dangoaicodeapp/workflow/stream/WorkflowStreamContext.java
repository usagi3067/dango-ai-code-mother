package com.dango.dangoaicodeapp.workflow.stream;

import reactor.core.publisher.FluxSink;

/**
 * 工作流流式输出上下文
 * 使用 ThreadLocal 在工作流执行过程中传递 FluxSink，
 * 使得各个节点可以实时输出信息到前端
 * 
 * @deprecated 由于 ThreadLocal 在虚拟线程和 langgraph4j 异步节点执行中无法正常工作，
 *             请改用 {@link com.dango.dangoaicodeapp.workflow.state.WorkflowContext#emit(String)} 方法。
 *             FluxSink 现在直接存储在 WorkflowContext 中，通过 context.emit() 发送消息。
 */
@Deprecated
public class WorkflowStreamContext {

    /**
     * ThreadLocal 存储当前线程的 FluxSink
     */
    private static final ThreadLocal<FluxSink<String>> SINK_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的 FluxSink
     *
     * @param sink FluxSink 对象
     */
    public static void setSink(FluxSink<String> sink) {
        SINK_HOLDER.set(sink);
    }

    /**
     * 获取当前线程的 FluxSink
     *
     * @return FluxSink 对象，可能为 null
     */
    public static FluxSink<String> getSink() {
        return SINK_HOLDER.get();
    }

    /**
     * 清除当前线程的 FluxSink
     */
    public static void clear() {
        SINK_HOLDER.remove();
    }

    /**
     * 发送消息到流（如果 sink 存在）
     *
     * @param message 要发送的消息
     */
    public static void emit(String message) {
        FluxSink<String> sink = SINK_HOLDER.get();
        if (sink != null) {
            sink.next(message);
        }
    }

    /**
     * 发送带前缀的节点消息
     *
     * @param nodeName 节点名称
     * @param message  消息内容
     */
    public static void emitNodeMessage(String nodeName, String message) {
        emit(String.format("[%s] %s", nodeName, message));
    }

    /**
     * 发送节点开始消息
     *
     * @param nodeName 节点名称
     */
    public static void emitNodeStart(String nodeName) {
        emitNodeMessage(nodeName, "开始执行...\n");
    }

    /**
     * 发送节点完成消息
     *
     * @param nodeName 节点名称
     */
    public static void emitNodeComplete(String nodeName) {
        emitNodeMessage(nodeName, "执行完成\n");
    }

    /**
     * 发送节点错误消息
     *
     * @param nodeName 节点名称
     * @param error    错误信息
     */
    public static void emitNodeError(String nodeName, String error) {
        emitNodeMessage(nodeName, "执行失败: " + error + "\n");
    }
}
