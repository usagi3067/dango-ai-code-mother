package com.dango.dangoaicodeapp.application.service.impl;

import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowStreamPort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.CodeGenWorkflow;
import com.dango.dangoaicodeapp.domain.codegen.workflow.command.RunWorkflowCommand;
import com.dango.dangoaicodecommon.monitor.MonitorContext;
import com.dango.dangoaicodecommon.monitor.MonitorContextHolder;
import com.dango.dangoaicodecommon.trace.TracedVirtualThread;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 工作流运行时编排器。
 * 负责流式 sink 生命周期与监控上下文管理，领域层只处理业务编排。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGenWorkflowExecutor {

    private final CodeGenWorkflow codeGenWorkflow;
    private final WorkflowStreamPort workflowStreamPort;

    public Flux<String> executeWithFlux(RunWorkflowCommand command, MonitorContext monitorContext) {
        return Flux.create(sink -> {
            String executionId = command.appId() + "_" + System.currentTimeMillis();
            workflowStreamPort.register(executionId, sink);

            TracedVirtualThread.start(() -> {
                try {
                    if (monitorContext != null) {
                        MonitorContextHolder.setContext(monitorContext);
                    }

                    RunWorkflowCommand executableCommand = command.withWorkflowExecutionId(executionId);
                    sink.next(JSONUtil.toJsonStr(new AiResponseMessage("[工作流] 开始处理请求...\n")));
                    codeGenWorkflow.run(executableCommand);
                    sink.next(JSONUtil.toJsonStr(new AiResponseMessage("[工作流] 全部流程执行完成！\n")));
                    sink.complete();
                } catch (Exception e) {
                    log.error("工作流执行失败: {}", e.getMessage(), e);
                    sink.error(e);
                } finally {
                    MonitorContextHolder.clearContext();
                    workflowStreamPort.unregister(executionId);
                }
            });
        });
    }
}
