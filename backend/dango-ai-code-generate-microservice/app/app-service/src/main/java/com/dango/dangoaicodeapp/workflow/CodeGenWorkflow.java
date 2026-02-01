package com.dango.dangoaicodeapp.workflow;

import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.workflow.node.*;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

@Slf4j
public class CodeGenWorkflow {

    // ========== 节点 Key 常量 ==========
    private static final String NODE_IMAGE_COLLECTOR = "image_collector";
    private static final String NODE_PROMPT_ENHANCER = "prompt_enhancer";
    private static final String NODE_ROUTER = "router";
    private static final String NODE_CODE_GENERATOR = "code_generator";
    private static final String NODE_PROJECT_BUILDER = "project_builder";

    // ========== 条件边路由常量 ==========
    private static final String ROUTE_BUILD = "build";
    private static final String ROUTE_SKIP_BUILD = "skip_build";

    /**
     * 创建完整的工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // 添加节点
                    .addNode(NODE_IMAGE_COLLECTOR, ImageCollectorNode.create())
                    .addNode(NODE_PROMPT_ENHANCER, PromptEnhancerNode.create())
                    .addNode(NODE_ROUTER, RouterNode.create())
                    .addNode(NODE_CODE_GENERATOR, CodeGeneratorNode.create())
                    .addNode(NODE_PROJECT_BUILDER, ProjectBuilderNode.create())

                    // 添加边
                    .addEdge(START, NODE_IMAGE_COLLECTOR)
                    .addEdge(NODE_IMAGE_COLLECTOR, NODE_PROMPT_ENHANCER)
                    .addEdge(NODE_PROMPT_ENHANCER, NODE_ROUTER)
                    .addEdge(NODE_ROUTER, NODE_CODE_GENERATOR)
                    // 使用条件边：根据代码生成类型决定是否需要构建
                    .addConditionalEdges(NODE_CODE_GENERATOR,
                            edge_async(this::routeBuildOrSkip),
                            Map.of(
                                    ROUTE_BUILD, NODE_PROJECT_BUILDER,  // 需要构建
                                    ROUTE_SKIP_BUILD, END               // 跳过构建直接结束
                            ))
                    .addEdge(NODE_PROJECT_BUILDER, END)

                    // 编译工作流
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工作流创建失败");
        }
    }

    /**
     * 路由函数：决定代码生成后是否需要项目构建
     * HTML 和 MULTI_FILE 类型在代码生成节点已保存文件，无需构建
     * VUE_PROJECT 类型需要执行 npm install + npm run build
     */
    private String routeBuildOrSkip(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGenTypeEnum generationType = context.getGenerationType();

        // HTML 和 MULTI_FILE 类型不需要构建，直接结束
        if (generationType == CodeGenTypeEnum.HTML || generationType == CodeGenTypeEnum.MULTI_FILE) {
            log.info("代码生成类型为 {}，跳过项目构建", generationType.getText());
            // 跳过构建时，将生成目录设置为最终构建目录
            context.setBuildResultDir(context.getGeneratedCodeDir());
            return ROUTE_SKIP_BUILD;
        }

        // VUE_PROJECT 需要构建
        log.info("代码生成类型为 {}，进入项目构建节点", generationType.getText());
        return ROUTE_BUILD;
    }

    /**
     * 执行工作流
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        return executeWorkflow(originalPrompt, 0L);
    }

    /**
     * 执行工作流（支持 appId 参数）
     */
    public WorkflowContext executeWorkflow(String originalPrompt, Long appId) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();

        // 初始化 WorkflowContext
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("初始化")
                .appId(appId)
                .build();

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图:\n{}", graph.content());
        log.info("开始执行代码生成工作流");

        WorkflowContext finalContext = null;
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            // 显示当前状态
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("当前步骤上下文: {}", currentContext);
            }
            stepCounter++;
        }
        log.info("代码生成工作流执行完成！");
        return finalContext;
    }

    /**
     * 执行工作流（Flux 流式输出版本）
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt) {
        return executeWorkflowWithFlux(originalPrompt, 0L);
    }

    /**
     * 执行工作流（Flux 流式输出版本，支持 appId 参数）
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt, Long appId) {
        return Flux.create(sink -> {
            // 生成唯一的执行 ID
            String executionId = appId + "_" + System.currentTimeMillis();
            
            // 注册 sink 到全局注册表
            WorkflowContext.registerSink(executionId, sink);
            
            Thread.startVirtualThread(() -> {
                try {
                    CompiledGraph<MessagesState<String>> workflow = createWorkflow();

                    // 初始化 WorkflowContext，传入 appId 和 executionId
                    WorkflowContext initialContext = WorkflowContext.builder()
                            .appId(appId)
                            .originalPrompt(originalPrompt)
                            .currentStep("初始化")
                            .workflowExecutionId(executionId)  // 关联执行 ID
                            .build();

                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("工作流图:\n{}", graph.content());
                    log.info("开始执行代码生成工作流（流式）, appId: {}, executionId: {}", appId, executionId);

                    // 发送工作流开始消息
                    sink.next("[工作流] 开始处理请求...\n");

                    int stepCounter = 1;
                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                        log.info("--- 第 {} 步完成 ---", stepCounter);
                        WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                        if (currentContext != null) {
                            log.info("当前步骤上下文: {}", currentContext);
                        }
                        stepCounter++;
                    }

                    log.info("代码生成工作流执行完成！");
                    sink.next("[工作流] 全部流程执行完成！\n");
                    sink.complete();
                } catch (Exception e) {
                    log.error("工作流执行失败: {}", e.getMessage(), e);
                    sink.error(e);
                } finally {
                    // 清理注册表
                    WorkflowContext.unregisterSink(executionId);
                }
            });
        });
    }

}