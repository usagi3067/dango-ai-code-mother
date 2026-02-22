package com.dango.dangoaicodeapp.domain.codegen.workflow;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.json.JSONUtil;
import com.dango.aicodegenerate.model.QualityResult;
import com.dango.aicodegenerate.model.message.AiResponseMessage;
import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.domain.app.valueobject.ElementInfo;
import com.dango.dangoaicodeapp.infrastructure.monitor.MonitorContext;
import com.dango.dangoaicodeapp.domain.codegen.node.*;
import com.dango.dangoaicodeapp.domain.codegen.node.concurrent.*;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
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
 * 使用 LangGraph4j 的子图能力实现多模式分离
 *
 * 主工作流：
 * START → mode_router → [条件边]
 *                       ├── create_subgraph          → build_check_subgraph → END
 *                       ├── leetcode_create_subgraph  → build_check_subgraph → END
 *                       ├── interview_create_subgraph → build_check_subgraph → END
 *                       └── existing_code_subgraph    → [条件边]
 *                                                      ├── (MODIFY) → build_check_subgraph → END
 *                                                      └── (QA)     → END
 *
 * 创建模式子图：
 *   image_plan → [并发图片收集] → image_aggregator → prompt_enhancer → code_generator
 *
 * 力扣创建模式子图：
 *   animation_advisor → leetcode_prompt_enhancer → code_generator
 *
 * 面试题解创建模式子图：
 *   interview_animation_advisor → interview_prompt_enhancer → code_generator
 *
 * 已有代码子图（展平，LangGraph4j 不支持子图嵌套）：
 *   code_reader → intent_classifier → [条件边]
 *                                     ├── modification_planner → [条件边] → database_operator → code_modifier
 *                                     │                          └── (skip_sql) → code_modifier
 *                                     └── qa_node
 *
 * 构建检查修复子图：
 *   build_check ←→ code_fixer（循环修复）
 */
@Slf4j
public class CodeGenWorkflow {

    // ========== 节点 Key 常量 ==========

    // 主工作流
    private static final String NODE_MODE_ROUTER = "mode_router";

    // 子图
    private static final String SUBGRAPH_CREATE = "create_subgraph";
    private static final String SUBGRAPH_LEETCODE_CREATE = "leetcode_create_subgraph";
    private static final String SUBGRAPH_INTERVIEW_CREATE = "interview_create_subgraph";
    private static final String SUBGRAPH_EXISTING_CODE = "existing_code_subgraph";
    private static final String SUBGRAPH_BUILD_CHECK = "build_check_subgraph";

    // 创建模式子图
    private static final String NODE_IMAGE_PLAN = "image_plan";
    private static final String NODE_CONTENT_IMAGE_COLLECTOR = "content_image_collector";
    private static final String NODE_ILLUSTRATION_COLLECTOR = "illustration_collector";
    private static final String NODE_DIAGRAM_COLLECTOR = "diagram_collector";
    private static final String NODE_LOGO_COLLECTOR = "logo_collector";
    private static final String NODE_IMAGE_AGGREGATOR = "image_aggregator";
    private static final String NODE_PROMPT_ENHANCER = "prompt_enhancer";
    private static final String NODE_CODE_GENERATOR = "code_generator";

    // 力扣创建模式子图
    private static final String NODE_LEETCODE_PROMPT_ENHANCER = "leetcode_prompt_enhancer";
    private static final String NODE_ANIMATION_ADVISOR = "animation_advisor";

    // 面试题解创建模式子图
    private static final String NODE_INTERVIEW_ANIMATION_ADVISOR = "interview_animation_advisor";
    private static final String NODE_INTERVIEW_PROMPT_ENHANCER = "interview_prompt_enhancer";

    // 已有代码子图（含展平的修改流程）
    private static final String NODE_CODE_READER = "code_reader";
    private static final String NODE_INTENT_CLASSIFIER = "intent_classifier";
    private static final String NODE_QA = "qa_node";
    private static final String NODE_MODIFICATION_PLANNER = "modification_planner";
    private static final String NODE_DATABASE_OPERATOR = "database_operator";
    private static final String NODE_CODE_MODIFIER = "code_modifier";

    // 构建检查修复子图
    private static final String NODE_BUILD_CHECK = "build_check";
    private static final String NODE_CODE_FIXER = "code_fixer";

    // ========== 条件边路由常量 ==========

    private static final String ROUTE_CREATE = "create";
    private static final String ROUTE_LEETCODE_CREATE = "leetcode_create";
    private static final String ROUTE_INTERVIEW_CREATE = "interview_create";
    private static final String ROUTE_EXISTING_CODE = "existing_code";
    private static final String ROUTE_MODIFY = "modify";
    private static final String ROUTE_QA = "qa";
    private static final String ROUTE_EXECUTE_SQL = "execute_sql";
    private static final String ROUTE_SKIP_SQL = "skip_sql";
    private static final String ROUTE_FIX = "fix";
    private static final String ROUTE_PASS = "pass";

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
     * 2. 根据模式路由到对应子图
     * 3. 创建类子图和已有代码修改分支汇聚到构建检查修复子图
     * 4. 已有代码问答分支直接结束（不需要构建检查）
     *
     * 注意：使用 addSubgraph 方法添加未编译的子图，确保上下文正确传递
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() throws GraphStateException {
        // 获取未编译的子图
        StateGraph<MessagesState<String>> createSubGraph = buildCreateModeSubGraph();
        StateGraph<MessagesState<String>> leetCodeCreateSubGraph = buildLeetCodeCreateSubGraph();
        StateGraph<MessagesState<String>> interviewCreateSubGraph = buildInterviewCreateSubGraph();
        StateGraph<MessagesState<String>> existingCodeSubGraph = buildExistingCodeSubGraph();
        StateGraph<MessagesState<String>> buildCheckSubGraph = buildBuildCheckSubGraph();

        return new MessagesStateGraph<String>()
                // 模式路由节点
                .addNode(NODE_MODE_ROUTER, ModeRouterNode.create())

                // 添加子图（使用 addNode 方法添加未编译的子图，确保上下文传递）
                .addNode(SUBGRAPH_CREATE, createSubGraph)
                .addNode(SUBGRAPH_LEETCODE_CREATE, leetCodeCreateSubGraph)
                .addNode(SUBGRAPH_INTERVIEW_CREATE, interviewCreateSubGraph)
                .addNode(SUBGRAPH_EXISTING_CODE, existingCodeSubGraph)
                .addNode(SUBGRAPH_BUILD_CHECK, buildCheckSubGraph)

                // 入口边
                .addEdge(START, NODE_MODE_ROUTER)

                // 模式路由条件边
                .addConditionalEdges(NODE_MODE_ROUTER,
                        edge_async(ModeRouterNode::routeToNextNode),
                        Map.of(
                                ROUTE_CREATE, SUBGRAPH_CREATE,
                                ROUTE_LEETCODE_CREATE, SUBGRAPH_LEETCODE_CREATE,
                                ROUTE_INTERVIEW_CREATE, SUBGRAPH_INTERVIEW_CREATE,
                                ROUTE_EXISTING_CODE, SUBGRAPH_EXISTING_CODE
                        ))

                // 创建类子图出口汇聚到构建检查子图
                .addEdge(SUBGRAPH_CREATE, SUBGRAPH_BUILD_CHECK)
                .addEdge(SUBGRAPH_LEETCODE_CREATE, SUBGRAPH_BUILD_CHECK)
                .addEdge(SUBGRAPH_INTERVIEW_CREATE, SUBGRAPH_BUILD_CHECK)

                // 已有代码子图出口：修改分支需要构建检查，问答分支直接结束
                .addConditionalEdges(SUBGRAPH_EXISTING_CODE,
                        edge_async(this::routeAfterExistingCode),
                        Map.of(
                                ROUTE_MODIFY, SUBGRAPH_BUILD_CHECK,
                                ROUTE_QA, END
                        ))

                // 构建检查子图完成后直接结束
                .addEdge(SUBGRAPH_BUILD_CHECK, END)

                // 编译工作流
                .compile();
    }

    // ========== 子图构建 ==========

    /**
     * 创建模式子图：图片规划 → 并发图片收集 → 聚合 → 提示词增强 → 代码生成
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
                .addEdge(NODE_PROMPT_ENHANCER, NODE_CODE_GENERATOR)

                // 出口
                .addEdge(NODE_CODE_GENERATOR, END);
    }

    /**
     * 力扣创建模式子图：动画设计建议 → 提示词增强 → 代码生成
     */
    private StateGraph<MessagesState<String>> buildLeetCodeCreateSubGraph() throws GraphStateException {
        return new MessagesStateGraph<String>()
                .addNode(NODE_ANIMATION_ADVISOR, LeetCodeAnimationAdvisorNode.create())
                .addNode(NODE_LEETCODE_PROMPT_ENHANCER, LeetCodePromptEnhancerNode.create())
                .addNode(NODE_CODE_GENERATOR, CodeGeneratorNode.create())
                .addEdge(START, NODE_ANIMATION_ADVISOR)
                .addEdge(NODE_ANIMATION_ADVISOR, NODE_LEETCODE_PROMPT_ENHANCER)
                .addEdge(NODE_LEETCODE_PROMPT_ENHANCER, NODE_CODE_GENERATOR)
                .addEdge(NODE_CODE_GENERATOR, END);
    }

    /**
     * 面试题解创建模式子图：动画设计建议 → 提示词增强 → 代码生成
     */
    private StateGraph<MessagesState<String>> buildInterviewCreateSubGraph() throws GraphStateException {
        return new MessagesStateGraph<String>()
                .addNode(NODE_INTERVIEW_ANIMATION_ADVISOR, InterviewAnimationAdvisorNode.create())
                .addNode(NODE_INTERVIEW_PROMPT_ENHANCER, InterviewPromptEnhancerNode.create())
                .addNode(NODE_CODE_GENERATOR, CodeGeneratorNode.create())
                .addEdge(START, NODE_INTERVIEW_ANIMATION_ADVISOR)
                .addEdge(NODE_INTERVIEW_ANIMATION_ADVISOR, NODE_INTERVIEW_PROMPT_ENHANCER)
                .addEdge(NODE_INTERVIEW_PROMPT_ENHANCER, NODE_CODE_GENERATOR)
                .addEdge(NODE_CODE_GENERATOR, END);
    }

    /**
     * 已有代码子图（修改流程展平，LangGraph4j 不支持子图嵌套）：
     * code_reader → intent_classifier → [MODIFY: planner → db → modifier | QA: qa_node]
     */
    private StateGraph<MessagesState<String>> buildExistingCodeSubGraph() throws GraphStateException {
        // 获取 DatabaseOperatorNode Bean（因为它需要 Dubbo 注入）
        DatabaseOperatorNode databaseOperatorNode = com.dango.dangoaicodecommon.utils.SpringContextUtil
                .getBean(DatabaseOperatorNode.class);

        return new MessagesStateGraph<String>()
                // 代码读取节点
                .addNode(NODE_CODE_READER, CodeReaderNode.create())

                // 意图识别节点
                .addNode(NODE_INTENT_CLASSIFIER, IntentClassifierNode.create())

                // 修改流程节点（展平）
                .addNode(NODE_MODIFICATION_PLANNER, ModificationPlannerNode.create())
                .addNode(NODE_DATABASE_OPERATOR, databaseOperatorNode.create())
                .addNode(NODE_CODE_MODIFIER, CodeModifierNode.create())

                // 问答节点
                .addNode(NODE_QA, QANode.create())

                // 入口边
                .addEdge(START, NODE_CODE_READER)

                // 读取后进行意图识别
                .addEdge(NODE_CODE_READER, NODE_INTENT_CLASSIFIER)

                // 意图识别条件边
                .addConditionalEdges(NODE_INTENT_CLASSIFIER,
                        edge_async(IntentClassifierNode::routeByIntent),
                        Map.of(
                                ROUTE_MODIFY, NODE_MODIFICATION_PLANNER,
                                ROUTE_QA, NODE_QA
                        ))

                // 修改规划后的条件边：根据 sqlStatements 决定是否执行 SQL
                .addConditionalEdges(NODE_MODIFICATION_PLANNER,
                        edge_async(this::routeAfterModificationPlanning),
                        Map.of(
                                ROUTE_EXECUTE_SQL, NODE_DATABASE_OPERATOR,
                                ROUTE_SKIP_SQL, NODE_CODE_MODIFIER
                        ))

                // 数据库操作完成后进入代码修改
                .addEdge(NODE_DATABASE_OPERATOR, NODE_CODE_MODIFIER)

                // 出口
                .addEdge(NODE_CODE_MODIFIER, END)
                .addEdge(NODE_QA, END);
    }

    /**
     * 构建检查修复子图：build_check ←→ code_fixer（循环修复，达上限则强制通过）
     */
    private StateGraph<MessagesState<String>> buildBuildCheckSubGraph() throws GraphStateException {
        return new MessagesStateGraph<String>()
                // 构建检查节点
                .addNode(NODE_BUILD_CHECK, BuildCheckNode.create())

                // 修复节点
                .addNode(NODE_CODE_FIXER, CodeFixerNode.create())

                // 入口边
                .addEdge(START, NODE_BUILD_CHECK)

                // 构建检查条件边（内部循环）
                .addConditionalEdges(NODE_BUILD_CHECK,
                        edge_async(this::routeInBuildCheck),
                        Map.of(
                                ROUTE_FIX, NODE_CODE_FIXER,
                                ROUTE_PASS, END
                        ))

                // 修复后重新构建检查
                .addEdge(NODE_CODE_FIXER, NODE_BUILD_CHECK);
    }

    // ========== 条件边路由方法 ==========

    /**
     * 已有代码子图出口路由：修改分支需要构建检查，问答分支直接结束
     */
    private String routeAfterExistingCode(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        String intent = context.getIntentType();
        if ("QA".equals(intent)) {
            log.info("问答模式，跳过构建检查");
            return ROUTE_QA;
        }
        log.info("修改模式，进入构建检查");
        return ROUTE_MODIFY;
    }

    /**
     * 修改规划后路由：有 SQL 则执行，否则跳过
     */
    private String routeAfterModificationPlanning(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);

        if (context.getModificationPlan() == null) {
            log.info("修改规划为空，跳过 SQL 执行");
            return ROUTE_SKIP_SQL;
        }

        if (context.getModificationPlan().getSqlStatements() == null
            || context.getModificationPlan().getSqlStatements().isEmpty()) {
            log.info("无需执行 SQL，跳过数据库操作节点");
            return ROUTE_SKIP_SQL;
        }

        log.info("有 {} 条 SQL 需要执行，路由到数据库操作节点",
            context.getModificationPlan().getSqlStatements().size());
        return ROUTE_EXECUTE_SQL;
    }

    /**
     * 构建检查路由：失败且未达上限则修复，否则通过
     */
    private String routeInBuildCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();

        if (qualityResult == null || !qualityResult.getIsValid()) {
            if (context.getFixRetryCount() < WorkflowContext.MAX_FIX_RETRY_COUNT) {
                log.info("构建未通过，路由到修复节点 (重试次数: {}/{})",
                        context.getFixRetryCount(), WorkflowContext.MAX_FIX_RETRY_COUNT);
                return ROUTE_FIX;
            } else {
                log.warn("达到最大修复重试次数 ({})，强制通过构建检查", WorkflowContext.MAX_FIX_RETRY_COUNT);
            }
        }

        log.info("构建通过或达到最大重试次数，路由到出口");
        return ROUTE_PASS;
    }

    // ========== 工作流执行 ==========

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

        if (log.isDebugEnabled()) {
            GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
            log.debug("工作流图:\n{}", graph.content());
        }
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
                log.debug("当前步骤上下文: {}", currentContext);
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
        return executeWorkflowWithFlux(originalPrompt, appId, elementInfo, false, null, monitorContext, null);
    }

    /**
     * 执行工作流（Flux 流式输出版本，完整参数版本）
     *
     * @param originalPrompt 用户原始提示词
     * @param appId 应用 ID
     * @param elementInfo 选中的元素信息（用于修改模式）
     * @param databaseEnabled 是否启用数据库
     * @param databaseSchema 数据库表结构（如果启用）
     * @param monitorContext 监控上下文（用于跨线程传递监控信息）
     * @return 流式输出的代码内容
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt, Long appId, ElementInfo elementInfo,
                                                boolean databaseEnabled, String databaseSchema, MonitorContext monitorContext) {
        return executeWorkflowWithFlux(originalPrompt, appId, elementInfo, databaseEnabled, databaseSchema, monitorContext, null);
    }

    /**
     * 执行工作流（Flux 流式输出版本，完整参数版本，支持 generationType）
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt, Long appId, ElementInfo elementInfo,
                                                boolean databaseEnabled, String databaseSchema, MonitorContext monitorContext,
                                                CodeGenTypeEnum generationType) {
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

                    // 初始化 WorkflowContext，传入 appId、executionId、elementInfo、databaseEnabled、databaseSchema 和 monitorContext
                    WorkflowContext initialContext = WorkflowContext.builder()
                            .appId(appId)
                            .originalPrompt(originalPrompt)
                            .currentStep("初始化")
                            .workflowExecutionId(executionId)
                            .elementInfo(elementInfo)
                            .databaseEnabled(databaseEnabled)
                            .databaseSchema(databaseSchema)
                            .monitorContext(monitorContext)
                            .generationType(generationType != null ? generationType : CodeGenTypeEnum.VUE_PROJECT)
                            .build();

                    if (log.isDebugEnabled()) {
                        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                        log.debug("工作流图:\n{}", graph.content());
                    }
                    log.info("开始执行代码生成工作流（流式）, appId: {}, executionId: {}, hasElementInfo: {}, databaseEnabled: {}",
                            appId, executionId, elementInfo != null, databaseEnabled);

                    // 发送工作流开始消息（包装为 JSON 格式）
                    String startMessage = "[工作流] 开始处理请求...\n";
                    sink.next(JSONUtil.toJsonStr(new AiResponseMessage(startMessage)));

                    int stepCounter = 1;
                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext),
                            runnableConfig)) {
                        log.info("--- 第 {} 步完成 ---", stepCounter);
                        WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                        if (currentContext != null) {
                            log.debug("当前步骤上下文: {}", currentContext);
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
     * 关闭线程池
     */
    public void shutdown() {
        if (parallelExecutor != null && !parallelExecutor.isShutdown()) {
            parallelExecutor.shutdown();
        }
    }
}
