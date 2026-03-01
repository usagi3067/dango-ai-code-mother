package com.dango.dangoaicodeapp.infrastructure.workflow;

import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowStreamPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.FluxSink;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流流式输出端口实现。
 */
@Component
public class WorkflowStreamPortImpl implements WorkflowStreamPort {

    private final Map<String, FluxSink<String>> sinkRegistry = new ConcurrentHashMap<>();

    @Override
    public void register(String executionId, FluxSink<String> sink) {
        sinkRegistry.put(executionId, sink);
    }

    @Override
    public void unregister(String executionId) {
        sinkRegistry.remove(executionId);
    }

    @Override
    public void emit(String executionId, String payload) {
        if (executionId == null) {
            return;
        }
        FluxSink<String> sink = sinkRegistry.get(executionId);
        if (sink != null) {
            sink.next(payload);
        }
    }
}
