package com.dango.dangoaicodeapp.infrastructure.workflow;

import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowStreamPort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 启动时注册全局流式端口。
 * 由 WorkflowContext 通过 static 引用发送节点流式消息。
 */
@Component
@RequiredArgsConstructor
public class WorkflowStreamPortConfigurer {

    private final WorkflowStreamPort workflowStreamPort;

    @PostConstruct
    public void init() {
        WorkflowContext.configureWorkflowStreamPort(workflowStreamPort);
    }
}
