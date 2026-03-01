package com.dango.dangoaicodeapp.domain.codegen.workflow;

import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowStreamPort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.command.RunWorkflowCommand;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * 代码生成工作流执行器。
 */
@Slf4j
public class CodeGenWorkflow {

    private static final String NODE_IMAGE_PLAN = "image_plan";

    private final ExecutorService parallelExecutor;
    private final CodeGenWorkflowFactory workflowFactory;
    private final WorkflowStreamPort workflowStreamPort;

    public CodeGenWorkflow(
            ExecutorService parallelExecutor,
            CodeGenWorkflowFactory workflowFactory,
            WorkflowStreamPort workflowStreamPort) {
        this.parallelExecutor = Objects.requireNonNull(parallelExecutor, "parallelExecutor");
        this.workflowFactory = Objects.requireNonNull(workflowFactory, "workflowFactory");
        this.workflowStreamPort = Objects.requireNonNull(workflowStreamPort, "workflowStreamPort");
    }

    private RunnableConfig createRunnableConfig() {
        return RunnableConfig.builder()
                .addParallelNodeExecutor(NODE_IMAGE_PLAN, parallelExecutor)
                .build();
    }

    public WorkflowContext run(RunWorkflowCommand command) {
        CompiledGraph<MessagesState<String>> workflow;
        try {
            workflow = workflowFactory.createWorkflow();
        } catch (GraphStateException e) {
            log.error("创建工作流失败", e);
            throw new RuntimeException("创建工作流失败", e);
        }

        WorkflowContext initialContext = WorkflowContext.builder()
                .appId(command.appId())
                .originalPrompt(command.originalPrompt())
                .currentStep("初始化")
                .workflowExecutionId(command.workflowExecutionId())
                .workflowStreamPort(workflowStreamPort)
                .elementInfo(command.elementInfo())
                .databaseEnabled(command.databaseEnabled())
                .databaseSchema(command.databaseSchema())
                .generationType(command.generationType())
                .build();

        if (log.isDebugEnabled()) {
            GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
            log.debug("工作流图:\n{}", graph.content());
        }
        log.info("开始执行代码生成工作流, appId: {}, hasElementInfo: {}, databaseEnabled: {}",
                command.appId(), command.elementInfo() != null, command.databaseEnabled());

        WorkflowContext finalContext = null;
        int stepCounter = 1;

        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext),
                createRunnableConfig())) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.debug("当前步骤上下文: {}", currentContext);
            }
            stepCounter++;
        }

        log.info("代码生成工作流执行完成！");
        return finalContext;
    }

}
