package com.dango.dangoaicodeapp.workflow;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.dango.aicodegenerate.model.QualityResult;
import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.workflow.node.*;
import com.dango.dangoaicodeapp.workflow.node.concurrent.*;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 代码生成工作流
 * 使用 LangGraph4j 的并发能力实现图片收集的并发执行
 * 
 * 工作流结构：
 * START → image_plan → [并发分支]
 *                      ├── content_image_collector ─┐
 *                      ├── illustration_collector ──┼── image_aggregator → prompt_enhancer → router → code_generator → code_quality_check → [条件边] → project_builder/END
 *                      ├── diagram_collector ───────┤
 *                      └── logo_collector ──────────┘
 */
@Slf4j
public class CodeGenWorkflow {

    // ========== 节点 Key 常量 ==========
    private static final String NODE_IMAGE_PLAN = "image_plan";
    private static final String NODE_CONTENT_IMAGE_COLLECTOR = "content_image_collector";
    private static final String NODE_ILLUSTRATION_COLLECTOR = "illustration_collector";
    private static final String NODE_DIAGRAM_COLLECTOR = "diagram_collector";
    private static final String NODE_LOGO_COLLECTOR = "logo_collector";
    private static final String NODE_IMAGE_AGGREGATOR = "image_aggregator";
    private static final String NODE_PROMPT_ENHANCER = "prompt_enhancer";
    private static final String NODE_ROUTER = "router";
    private static final String NODE_CODE_GENERATOR = "code_generator";
    private static final String NODE_CODE_QUALITY_CHECK = "code_quality_check";
    private static final String NODE_PROJECT_BUILDER = "project_builder";

    // ========== 条件边路由常量 ==========
    private static final String ROUTE_BUILD = "build";
    private static final String ROUTE_SKIP_BUILD = "skip_build";
    private static final String ROUTE_FAIL = "fail";

    /**
     * 并发执行线程池
     */
    private final ExecutorService parallelExecutor;

    public CodeGenWorkflow() {
        // 配置并发执行线程池
        this.parallelExecutor = ExecutorBuilder.create()
                .setCorePoolSize(10)
                .setMaxPoolSize(20)
                .setWorkQueue(new LinkedBlockingQueue<>(100))
                .setThreadFactory(ThreadFactoryBuilder.create()
                        .setNamePrefix("Parallel-Image-Collect-")
                        .build())
                .build();
    }

    /**
     * 创建工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // 添加图片规划节点
                    .addNode(NODE_IMAGE_PLAN, ImagePlanNode.create())

                    // 添加并发图片收集节点
                    .addNode(NODE_CONTENT_IMAGE_COLLECTOR, ContentImageCollectorNode.create())
                    .addNode(NODE_ILLUSTRATION_COLLECTOR, IllustrationCollectorNode.create())
                    .addNode(NODE_DIAGRAM_COLLECTOR, DiagramCollectorNode.create())
                    .addNode(NODE_LOGO_COLLECTOR, LogoCollectorNode.create())

                    // 添加图片聚合节点
                    .addNode(NODE_IMAGE_AGGREGATOR, ImageAggregatorNode.create())

                    // 添加后续处理节点
                    .addNode(NODE_PROMPT_ENHANCER, PromptEnhancerNode.create())
                    .addNode(NODE_ROUTER, RouterNode.create())
                    .addNode(NODE_CODE_GENERATOR, CodeGeneratorNode.create())
                    .addNode(NODE_CODE_QUALITY_CHECK, CodeQualityCheckNode.create())
                    .addNode(NODE_PROJECT_BUILDER, ProjectBuilderNode.create())

                    // 添加边：开始 -> 图片规划
                    .addEdge(START, NODE_IMAGE_PLAN)

                    // 并发分支：从计划节点分发到各个收集节点
                    .addEdge(NODE_IMAGE_PLAN, NODE_CONTENT_IMAGE_COLLECTOR)
                    .addEdge(NODE_IMAGE_PLAN, NODE_ILLUSTRATION_COLLECTOR)
                    .addEdge(NODE_IMAGE_PLAN, NODE_DIAGRAM_COLLECTOR)
                    .addEdge(NODE_IMAGE_PLAN, NODE_LOGO_COLLECTOR)

                    // 汇聚：所有收集节点都汇聚到聚合器
                    .addEdge(NODE_CONTENT_IMAGE_COLLECTOR, NODE_IMAGE_AGGREGATOR)
                    .addEdge(NODE_ILLUSTRATION_COLLECTOR, NODE_IMAGE_AGGREGATOR)
                    .addEdge(NODE_DIAGRAM_COLLECTOR, NODE_IMAGE_AGGREGATOR)
                    .addEdge(NODE_LOGO_COLLECTOR, NODE_IMAGE_AGGREGATOR)

                    // 继续串行流程
                    .addEdge(NODE_IMAGE_AGGREGATOR, NODE_PROMPT_ENHANCER)
                    .addEdge(NODE_PROMPT_ENHANCER, NODE_ROUTER)
                    .addEdge(NODE_ROUTER, NODE_CODE_GENERATOR)
                    .addEdge(NODE_CODE_GENERATOR, NODE_CODE_QUALITY_CHECK)

                    // 质检条件边
                    .addConditionalEdges(NODE_CODE_QUALITY_CHECK,
                            edge_async(this::routeAfterQualityCheck),
                            Map.of(
                                    ROUTE_BUILD, NODE_PROJECT_BUILDER,
                                    ROUTE_SKIP_BUILD, END,
                                    ROUTE_FAIL, NODE_CODE_GENERATOR
                            ))
                    .addEdge(NODE_PROJECT_BUILDER, END)

                    // 编译工作流
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工作流创建失败");
        }
    }

    /**
     * 创建并发执行配置
     */
    private RunnableConfig createRunnableConfig() {
        return RunnableConfig.builder()
                .addParallelNodeExecutor(NODE_IMAGE_PLAN, parallelExecutor)
                .build();
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
        RunnableConfig runnableConfig = createRunnableConfig();

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
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext),
                runnableConfig)) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
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
                    RunnableConfig runnableConfig = createRunnableConfig();

                    // 初始化 WorkflowContext，传入 appId 和 executionId
                    WorkflowContext initialContext = WorkflowContext.builder()
                            .appId(appId)
                            .originalPrompt(originalPrompt)
                            .currentStep("初始化")
                            .workflowExecutionId(executionId)
                            .build();

                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("工作流图:\n{}", graph.content());
                    log.info("开始执行代码生成工作流（流式）, appId: {}, executionId: {}", appId, executionId);

                    // 发送工作流开始消息
                    sink.next("[工作流] 开始处理请求...\n");

                    int stepCounter = 1;
                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext),
                            runnableConfig)) {
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

    /**
     * 路由函数：根据质检结果决定下一步
     */
    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();

        if (qualityResult == null || !qualityResult.getIsValid()) {
            log.error("代码质检失败，需要重新生成代码");
            return ROUTE_FAIL;
        }

        log.info("代码质检通过，继续后续流程");
        CodeGenTypeEnum generationType = context.getGenerationType();

        if (generationType == CodeGenTypeEnum.VUE_PROJECT) {
            return ROUTE_BUILD;
        } else {
            // 跳过构建时，将生成目录设置为最终构建目录
            context.setBuildResultDir(context.getGeneratedCodeDir());
            return ROUTE_SKIP_BUILD;
        }
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        if (parallelExecutor != null && !parallelExecutor.isShutdown()) {
            parallelExecutor.shutdown();
        }
    }
}
