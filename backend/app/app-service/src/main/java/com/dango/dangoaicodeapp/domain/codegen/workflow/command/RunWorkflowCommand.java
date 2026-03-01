package com.dango.dangoaicodeapp.domain.codegen.workflow.command;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.app.valueobject.ElementInfo;
import lombok.Builder;

/**
 * 工作流运行命令。
 * 统一聚合工作流所需入参，避免方法重载扩散。
 */
@Builder(toBuilder = true)
public record RunWorkflowCommand(
        String originalPrompt,
        Long appId,
        ElementInfo elementInfo,
        boolean databaseEnabled,
        String databaseSchema,
        CodeGenTypeEnum generationType,
        String workflowExecutionId
) {

    public RunWorkflowCommand {
        if (appId == null || appId <= 0) {
            appId = 0L;
        }
        if (generationType == null) {
            generationType = CodeGenTypeEnum.VUE_PROJECT;
        }
    }

    public RunWorkflowCommand withWorkflowExecutionId(String executionId) {
        return this.toBuilder().workflowExecutionId(executionId).build();
    }
}
