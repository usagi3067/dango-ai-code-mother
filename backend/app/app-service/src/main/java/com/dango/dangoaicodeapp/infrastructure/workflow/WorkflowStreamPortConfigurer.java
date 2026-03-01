package com.dango.dangoaicodeapp.infrastructure.workflow;

import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowStreamPort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 将基础设施层的流式端口接入到工作流上下文。
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
