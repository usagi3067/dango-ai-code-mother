package com.dango.dangoaicodeapp.domain.codegen.port;

import reactor.core.publisher.FluxSink;

/**
 * 工作流流式输出端口。
 * 由应用/基础设施层管理 sink 生命周期，领域层仅按 executionId 发送消息。
 */
public interface WorkflowStreamPort {

    void register(String executionId, FluxSink<String> sink);

    void unregister(String executionId);

    void emit(String executionId, String payload);
}
