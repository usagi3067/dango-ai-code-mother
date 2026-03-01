package com.dango.dangoaicodeapp.infrastructure.workflow;

import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowMessagePort;
import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowStreamPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 工作流消息输出端口实现。
 */
@Component
@RequiredArgsConstructor
public class WorkflowMessagePortImpl implements WorkflowMessagePort {

    private final WorkflowStreamPort workflowStreamPort;

    @Override
    public void emitRaw(String executionId, String payload) {
        if (executionId == null || payload == null) {
            return;
        }
        workflowStreamPort.emit(executionId, payload);
    }

    @Override
    public void emitNodeMessage(String executionId, String nodeName, String message) {
        emitLog(executionId, String.format("[%s] %s", nodeName, message));
    }

    @Override
    public void emitNodeStart(String executionId, String nodeName) {
        emitNodeMessage(executionId, nodeName, "开始执行...\n");
    }

    @Override
    public void emitNodeComplete(String executionId, String nodeName) {
        emitNodeMessage(executionId, nodeName, "执行完成\n");
    }

    @Override
    public void emitNodeError(String executionId, String nodeName, String error) {
        emitNodeMessage(executionId, nodeName, "执行失败: " + error + "\n");
    }

    private void emitLog(String executionId, String text) {
        AiResponseMessage message = new AiResponseMessage(text);
        message.setMsgType("log");
        emitRaw(executionId, JSONUtil.toJsonStr(message));
    }
}
