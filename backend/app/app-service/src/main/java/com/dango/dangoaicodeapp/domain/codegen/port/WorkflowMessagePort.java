package com.dango.dangoaicodeapp.domain.codegen.port;

/**
 * 工作流消息输出端口。
 * 将“节点日志/内容消息的序列化与输出”从 WorkflowContext 中移出，避免状态对象承载基础设施职责。
 */
public interface WorkflowMessagePort {

    void emitRaw(String executionId, String payload);

    void emitNodeMessage(String executionId, String nodeName, String message);

    void emitNodeStart(String executionId, String nodeName);

    void emitNodeComplete(String executionId, String nodeName);

    void emitNodeError(String executionId, String nodeName, String error);
}
