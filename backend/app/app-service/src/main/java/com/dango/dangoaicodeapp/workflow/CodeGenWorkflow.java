package com.dango.dangoaicodeapp.workflow;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.model.QualityResult;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.dangoaicodeapp.model.entity.ElementInfo;
import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.model.enums.OperationModeEnum;
import com.dango.dangoaicodeapp.monitor.MonitorContext;
import com.dango.dangoaicodeapp.workflow.node.*;
import com.dango.dangoaicodeapp.workflow.node.concurrent.*;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.dango.dangoaicodecommon.trace.TracedVirtualThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
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
 * 使用 LangGraph4j 的子图能力实现创建/修改模式分离
 *
 * 工作流结构（使用子图）：
 * START → mode_router → [条件边]
 *                       ├── create_subgraph (创建模式子图)
 *                       └── modify_subgraph (修改模式子图)
 *                              ↓
 *                       quality_check_subgraph (质检修复子图)
 *                              ↓
 *                       [条件边] → project_builder / END
 *
 * 创建模式子图：
 * image_plan → [并发分支] → image_aggregator → prompt_enhancer → router → code_generator
 *
 * 修改模式子图（支持数据库操作）：
 * code_reader → database_analyzer → [条件边] → database_operator → code_modifier
 *                                       ↓
 *                                 (无需SQL时跳过)
 *                                       ↓
 *                                 code_modifier
 *
 * 质检修复子图：
 * code_quality_check ←→ code_fixer (循环修复)
 */
@Slf4j
public class CodeGenWorkflow {

    // ========== 节点 Key 常量 ==========
    // 主工作流节点
    private static final String NODE_MODE_ROUTER = "mode_router";
    private static final String NODE_PROJECT_BUILDER = "project_builder";
    
    // 子图节点名称
    private static final String SUBGRAPH_CREATE = "create_subgraph";
    private static final String SUBGRAPH_MODIFY = "modify_subgraph";
    private static final String SUBGRAPH_QUALITY_CHECK = "quality_check_subgraph";
    
    // 创建模式子图节点
    private static final String NODE_IMAGE_PLAN = "image_plan";
    private static final String NODE_CONTENT_IMAGE_COLLECTOR = "content_image_collector";
    private static final String NODE_ILLUSTRATION_COLLECTOR = "illustration_collector";
    private static final String NODE_DIAGRAM_COLLECTOR = "diagram_collector";
    private static final String NODE_LOGO_COLLECTOR = "logo_collector";
    private static final String NODE_IMAGE_AGGREGATOR = "image_aggregator";
    private static final String NODE_PROMPT_ENHANCER = "prompt_enhancer";
    private static final String NODE_ROUTER = "router";
    private static final String NODE_CODE_GENERATOR = "code_generator";
    
    // 修改模式子图节点
    private static final String NODE_CODE_READER = "code_reader";
    private static final String NODE_DATABASE_ANALYZER = "database_analyzer";
    private static final String NODE_DATABASE_OPERATOR = "database_operator";
    private static final String NODE_CODE_MODIFIER = "code_modifier";
    
    // 质检修复子图节点
    private static final String NODE_CODE_QUALITY_CHECK = "code_quality_check";
    private static final String NODE_CODE_FIXER = "code_fixer";

    // ========== 条件边路由常量 ==========
    // 模式路由
    private static final String ROUTE_CREATE = "create";
    private static final String ROUTE_MODIFY = "modify";

    // 数据库操作路由
    private static final String ROUTE_EXECUTE_SQL = "execute_sql";
    private static final String ROUTE_SKIP_SQL = "skip_sql";

    // 质检子图内部路由
    private static final String ROUTE_FIX = "fix";
    private static final String ROUTE_PASS = "pass";
    
    // 质检子图出口路由
    private static final String ROUTE_BUILD = "build";
    private static final String ROUTE_END = "end";

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
     * 创建工作流（使用子图重构）
     * 
     * 工作流结构：
     * 1. 模式路由节点判断操作模式
     * 2. 根据模式路由到创建子图或修改子图
     * 3. 两个子图都汇聚到质检修复子图
     * 4. 质检通过后根据类型决定是否构建
     * 
     * 注意：使用 addSubgraph 方法添加未编译的子图，确保上下文正确传递
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() throws GraphStateException {
        // 获取未编译的子图
        StateGraph<MessagesState<String>> createSubGraph = buildCreateModeSubGraph();
        StateGraph<MessagesState<String>> modifySubGraph = buildModifyModeSubGraph();
        StateGraph<MessagesState<String>> qualityCheckSubGraph = buildQualityCheckSubGraph();
        
        return new MessagesStateGraph<String>()
                // 模式路由节点
                .addNode(NODE_MODE_ROUTER, ModeRouterNode.create())
                
                // 添加子图（使用 addNode 方法添加未编译的子图，确保上下文传递）
                .addNode(SUBGRAPH_CREATE, createSubGraph)
                .addNode(SUBGRAPH_MODIFY, modifySubGraph)
                .addNode(SUBGRAPH_QUALITY_CHECK, qualityCheckSubGraph)
                
                // 项目构建节点
                .addNode(NODE_PROJECT_BUILDER, ProjectBuilderNode.create())
                
                // 入口边
                .addEdge(START, NODE_MODE_ROUTER)
                
                // 模式路由条件边
                .addConditionalEdges(NODE_MODE_ROUTER,
                        edge_async(ModeRouterNode::routeToNextNode),
                        Map.of(
                                ROUTE_CREATE, SUBGRAPH_CREATE,
                                ROUTE_MODIFY, SUBGRAPH_MODIFY
                        ))
                
                // 子图出口汇聚到质检子图
                .addEdge(SUBGRAPH_CREATE, SUBGRAPH_QUALITY_CHECK)
                .addEdge(SUBGRAPH_MODIFY, SUBGRAPH_QUALITY_CHECK)
                
                // 质检子图出口条件边
                .addConditionalEdges(SUBGRAPH_QUALITY_CHECK,
                        edge_async(this::routeAfterQualityCheck),
                        Map.of(
                                ROUTE_BUILD, NODE_PROJECT_BUILDER,
                                ROUTE_END, END
                        ))
                
                // 构建完成
                .addEdge(NODE_PROJECT_BUILDER, END)
                
                // 编译工作流
                .compile();
    }
    
    /**
     * 构建创建模式子图
     * 
     * 包含节点：
     * - 图片规划节点
     * - 并发图片收集节点（内容图片、插画、架构图、Logo）
     * - 图片聚合节点
     * - 提示词增强节点
     * - 路由节点
     * - 代码生成节点
     * 
     * @return 创建模式子图
     */
    private StateGraph<MessagesState<String>> buildCreateModeSubGraph() throws GraphStateException {
        return new MessagesStateGraph<String>()
                // 图片规划节点
                .addNode(NODE_IMAGE_PLAN, ImagePlanNode.create())
                
                // 并发图片收集节点
                .addNode(NODE_CONTENT_IMAGE_COLLECTOR, ContentImageCollectorNode.create())
                .addNode(NODE_ILLUSTRATION_COLLECTOR, IllustrationCollectorNode.create())
                .addNode(NODE_DIAGRAM_COLLECTOR, DiagramCollectorNode.create())
                .addNode(NODE_LOGO_COLLECTOR, LogoCollectorNode.create())
                
                // 图片聚合节点
                .addNode(NODE_IMAGE_AGGREGATOR, ImageAggregatorNode.create())
                
                // 后续处理节点
                .addNode(NODE_PROMPT_ENHANCER, PromptEnhancerNode.create())
                .addNode(NODE_ROUTER, RouterNode.create())
                .addNode(NODE_CODE_GENERATOR, CodeGeneratorNode.create())
                
                // 入口边
                .addEdge(START, NODE_IMAGE_PLAN)
                
                // 并发图片收集边
                .addEdge(NODE_IMAGE_PLAN, NODE_CONTENT_IMAGE_COLLECTOR)
                .addEdge(NODE_IMAGE_PLAN, NODE_ILLUSTRATION_COLLECTOR)
                .addEdge(NODE_IMAGE_PLAN, NODE_DIAGRAM_COLLECTOR)
                .addEdge(NODE_IMAGE_PLAN, NODE_LOGO_COLLECTOR)
                
                // 汇聚到聚合器
                .addEdge(NODE_CONTENT_IMAGE_COLLECTOR, NODE_IMAGE_AGGREGATOR)
                .addEdge(NODE_ILLUSTRATION_COLLECTOR, NODE_IMAGE_AGGREGATOR)
                .addEdge(NODE_DIAGRAM_COLLECTOR, NODE_IMAGE_AGGREGATOR)
                .addEdge(NODE_LOGO_COLLECTOR, NODE_IMAGE_AGGREGATOR)
                
                // 串行流程
                .addEdge(NODE_IMAGE_AGGREGATOR, NODE_PROMPT_ENHANCER)
                .addEdge(NODE_PROMPT_ENHANCER, NODE_ROUTER)
                .addEdge(NODE_ROUTER, NODE_CODE_GENERATOR)
                
                // 出口
                .addEdge(NODE_CODE_GENERATOR, END);
    }
    
    /**
     * 构建修改模式子图
     *
     * 包含节点：
     * - 代码读取节点：读取现有项目结构
     * - 数据库分析节点：分析是否需要数据库操作（仅启用数据库时）
     * - 数据库操作节点：执行 SQL 语句（仅有 SQL 时）
     * - 代码修改节点：使用修改专用提示词进行增量修改
     *
     * 流程：
     * CodeReader → DatabaseAnalyzer → [条件边] → DatabaseOperator → CodeModifier
     *                                     ↓
     *                               (无需SQL时直接跳过)
     *                                     ↓
     *                               CodeModifier
     *
     * @return 修改模式子图
     */
    private StateGraph<MessagesState<String>> buildModifyModeSubGraph() throws GraphStateException {
        // 获取 DatabaseOperatorNode Bean（因为它需要 Dubbo 注入）
        DatabaseOperatorNode databaseOperatorNode = com.dango.dangoaicodecommon.utils.SpringContextUtil
                .getBean(DatabaseOperatorNode.class);

        return new MessagesStateGraph<String>()
                // 代码读取节点
                .addNode(NODE_CODE_READER, CodeReaderNode.create())

                // 数据库分析节点
                .addNode(NODE_DATABASE_ANALYZER, DatabaseAnalyzerNode.create())

                // 数据库操作节点（使用实例方法，因为需要 Dubbo 注入）
                .addNode(NODE_DATABASE_OPERATOR, databaseOperatorNode.create())

                // 代码修改节点
                .addNode(NODE_CODE_MODIFIER, CodeModifierNode.create())

                // 入口边
                .addEdge(START, NODE_CODE_READER)

                // 读取后进行数据库分析
                .addEdge(NODE_CODE_READER, NODE_DATABASE_ANALYZER)

                // 数据库分析后的条件边：根据 sqlStatements 决定是否执行 SQL
                .addConditionalEdges(NODE_DATABASE_ANALYZER,
                        edge_async(this::routeAfterDatabaseAnalysis),
                        Map.of(
                                ROUTE_EXECUTE_SQL, NODE_DATABASE_OPERATOR,
                                ROUTE_SKIP_SQL, NODE_CODE_MODIFIER
                        ))

                // 数据库操作完成后进入代码修改
                .addEdge(NODE_DATABASE_OPERATOR, NODE_CODE_MODIFIER)

                // 出口
                .addEdge(NODE_CODE_MODIFIER, END);
    }

    /**
     * 数据库分析后的路由逻辑
     *
     * 判断逻辑：
     * 1. 数据库未启用 → 跳过 SQL 执行
     * 2. sqlStatements 为空 → 跳过 SQL 执行
     * 3. 否则 → 执行 SQL
     *
     * @param state 消息状态
     * @return 路由目标（"execute_sql" 或 "skip_sql"）
     */
    private String routeAfterDatabaseAnalysis(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);

        // 数据库未启用，跳过
        if (!context.isDatabaseEnabled()) {
            log.info("数据库未启用，跳过 SQL 执行");
            return ROUTE_SKIP_SQL;
        }

        // 没有需要执行的 SQL，跳过
        if (context.getSqlStatements() == null || context.getSqlStatements().isEmpty()) {
            log.info("无需执行 SQL，跳过数据库操作节点");
            return ROUTE_SKIP_SQL;
        }

        log.info("有 {} 条 SQL 需要执行，路由到数据库操作节点", context.getSqlStatements().size());
        return ROUTE_EXECUTE_SQL;
    }
    
    /**
     * 构建质检修复子图
     * 
     * 包含节点：
     * - 代码质量检查节点
     * - 代码修复节点
     * 
     * 循环逻辑：
     * - 质检失败且未达最大重试次数 → 修复节点
     * - 修复完成 → 重新质检
     * - 质检通过或达到最大重试次数 → 出口
     * 
     * @return 质检修复子图
     */
    private StateGraph<MessagesState<String>> buildQualityCheckSubGraph() throws GraphStateException {
        return new MessagesStateGraph<String>()
                // 质检节点
                .addNode(NODE_CODE_QUALITY_CHECK, CodeQualityCheckNode.create())
                
                // 修复节点
                .addNode(NODE_CODE_FIXER, CodeFixerNode.create())
                
                // 入口边
                .addEdge(START, NODE_CODE_QUALITY_CHECK)
                
                // 质检条件边（内部循环）
                .addConditionalEdges(NODE_CODE_QUALITY_CHECK,
                        edge_async(this::routeInQualityCheck),
                        Map.of(
                                ROUTE_FIX, NODE_CODE_FIXER,
                                ROUTE_PASS, END
                        ))
                
                // 修复后重新质检
                .addEdge(NODE_CODE_FIXER, NODE_CODE_QUALITY_CHECK);
    }
    
    /**
     * 质检子图内部路由逻辑
     * 
     * 判断逻辑：
     * 1. 质检失败且未达最大重试次数 → 路由到修复节点
     * 2. 质检通过或达到最大重试次数 → 路由到出口
     * 
     * @param state 消息状态
     * @return 路由目标（"fix" 或 "pass"）
     */
    private String routeInQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();
        
        // 质检失败且未达最大重试次数，路由到修复节点
        if (qualityResult == null || !qualityResult.getIsValid()) {
            if (context.getFixRetryCount() < WorkflowContext.MAX_FIX_RETRY_COUNT) {
                log.info("质检未通过，路由到修复节点 (重试次数: {}/{})", 
                        context.getFixRetryCount(), WorkflowContext.MAX_FIX_RETRY_COUNT);
                return ROUTE_FIX;
            } else {
                log.warn("达到最大修复重试次数 ({})，强制通过质检", WorkflowContext.MAX_FIX_RETRY_COUNT);
            }
        }
        
        log.info("质检通过或达到最大重试次数，路由到出口");
        return ROUTE_PASS;
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
        return executeWorkflow(originalPrompt, 0L, null);
    }

    /**
     * 执行工作流（支持 appId 参数）
     */
    public WorkflowContext executeWorkflow(String originalPrompt, Long appId) {
        return executeWorkflow(originalPrompt, appId, null);
    }
    
    /**
     * 执行工作流（支持 appId 和 elementInfo 参数）
     * 
     * @param originalPrompt 用户原始提示词
     * @param appId 应用 ID
     * @param elementInfo 选中的元素信息（用于修改模式）
     * @return 工作流上下文
     */
    public WorkflowContext executeWorkflow(String originalPrompt, Long appId, ElementInfo elementInfo) {
        CompiledGraph<MessagesState<String>> workflow = null;
        try {
            workflow = createWorkflow();
        } catch (GraphStateException e) {
            log.error("创建工作流失败", e);
            throw new RuntimeException("创建工作流失败", e);
        }
        RunnableConfig runnableConfig = createRunnableConfig();

        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("初始化")
                .appId(appId)
                .elementInfo(elementInfo)
                .build();

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图:\n{}", graph.content());
        log.info("开始执行代码生成工作流, appId: {}, hasElementInfo: {}", appId, elementInfo != null);

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
        return executeWorkflowWithFlux(originalPrompt, 0L, null);
    }

    /**
     * 执行工作流（Flux 流式输出版本，支持 appId 参数）
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt, Long appId) {
        return executeWorkflowWithFlux(originalPrompt, appId, null);
    }
    
    /**
     * 执行工作流（Flux 流式输出版本，支持 appId 和 elementInfo 参数）
     * 
     * @param originalPrompt 用户原始提示词
     * @param appId 应用 ID
     * @param elementInfo 选中的元素信息（用于修改模式）
     * @return 流式输出的代码内容
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt, Long appId, ElementInfo elementInfo) {
        return executeWorkflowWithFlux(originalPrompt, appId, elementInfo, null);
    }
    
    /**
     * 执行工作流（Flux 流式输出版本，支持 appId、elementInfo 和 monitorContext 参数）
     * 
     * @param originalPrompt 用户原始提示词
     * @param appId 应用 ID
     * @param elementInfo 选中的元素信息（用于修改模式）
     * @param monitorContext 监控上下文（用于跨线程传递监控信息）
     * @return 流式输出的代码内容
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt, Long appId, ElementInfo elementInfo, MonitorContext monitorContext) {
        return Flux.create(sink -> {
            // 生成唯一的执行 ID
            String executionId = appId + "_" + System.currentTimeMillis();

            // 注册 sink 到全局注册表
            WorkflowContext.registerSink(executionId, sink);

            // 使用 TracedVirtualThread 自动传递 traceId
            TracedVirtualThread.start(() -> {
                try {
                    CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                    RunnableConfig runnableConfig = createRunnableConfig();

                    // 初始化 WorkflowContext，传入 appId、executionId、elementInfo 和 monitorContext
                    WorkflowContext initialContext = WorkflowContext.builder()
                            .appId(appId)
                            .originalPrompt(originalPrompt)
                            .currentStep("初始化")
                            .workflowExecutionId(executionId)
                            .elementInfo(elementInfo)
                            .monitorContext(monitorContext)
                            .build();

                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("工作流图:\n{}", graph.content());
                    log.info("开始执行代码生成工作流（流式）, appId: {}, executionId: {}, hasElementInfo: {}",
                            appId, executionId, elementInfo != null);

                    // 发送工作流开始消息（包装为 JSON 格式）
                    String modeHint = elementInfo != null ? "（修改模式）" : "（创建模式）";
                    String startMessage = "[工作流] 开始处理请求..." + modeHint + "\n";
                    sink.next(JSONUtil.toJsonStr(new AiResponseMessage(startMessage)));

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
                    // 发送工作流完成消息（包装为 JSON 格式）
                    String completeMessage = "[工作流] 全部流程执行完成！\n";
                    sink.next(JSONUtil.toJsonStr(new AiResponseMessage(completeMessage)));
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
     * 质检子图出口路由逻辑
     * 
     * 判断逻辑：
     * 1. 如果 codeGenType 为 VUE_PROJECT，需要构建 → 路由到 project_builder
     * 2. 否则不需要构建 → 路由到 END
     * 
     * @param state 消息状态
     * @return 路由目标（"build" 或 "end"）
     */
    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGenTypeEnum generationType = context.getGenerationType();

        if (generationType == CodeGenTypeEnum.VUE_PROJECT) {
            log.info("代码类型为 VUE_PROJECT，路由到项目构建节点");
            return ROUTE_BUILD;
        } else {
            // 跳过构建时，将生成目录设置为最终构建目录
            context.setBuildResultDir(context.getGeneratedCodeDir());
            log.info("代码类型为 {}，跳过构建，直接结束", generationType);
            return ROUTE_END;
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
