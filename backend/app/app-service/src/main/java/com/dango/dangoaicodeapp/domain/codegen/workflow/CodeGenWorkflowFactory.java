package com.dango.dangoaicodeapp.domain.codegen.workflow;

import com.dango.aicodegenerate.model.QualityResult;
import com.dango.dangoaicodeapp.domain.codegen.node.*;
import com.dango.dangoaicodeapp.domain.codegen.node.concurrent.*;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 代码生成工作流工厂。
 * 统一装配节点 Action，避免在工作流执行类中动态查找 Bean。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGenWorkflowFactory {

    private static final String NODE_MODE_ROUTER = "mode_router";

    private static final String SUBGRAPH_CREATE = "create_subgraph";
    private static final String SUBGRAPH_LEETCODE_CREATE = "leetcode_create_subgraph";
    private static final String SUBGRAPH_INTERVIEW_CREATE = "interview_create_subgraph";
    private static final String SUBGRAPH_INTERVIEW_SOURCE_CODE_CREATE = "interview_source_code_create_subgraph";
    private static final String SUBGRAPH_EXISTING_CODE = "existing_code_subgraph";
    private static final String SUBGRAPH_BUILD_CHECK = "build_check_subgraph";

    private static final String NODE_IMAGE_PLAN = "image_plan";
    private static final String NODE_CONTENT_IMAGE_COLLECTOR = "content_image_collector";
    private static final String NODE_ILLUSTRATION_COLLECTOR = "illustration_collector";
    private static final String NODE_DIAGRAM_COLLECTOR = "diagram_collector";
    private static final String NODE_LOGO_COLLECTOR = "logo_collector";
    private static final String NODE_IMAGE_AGGREGATOR = "image_aggregator";
    private static final String NODE_PROMPT_ENHANCER = "prompt_enhancer";
    private static final String NODE_CODE_GENERATOR = "code_generator";

    private static final String NODE_LEETCODE_PROMPT_ENHANCER = "leetcode_prompt_enhancer";
    private static final String NODE_ANIMATION_ADVISOR = "animation_advisor";

    private static final String NODE_INTERVIEW_ANIMATION_ADVISOR = "interview_animation_advisor";
    private static final String NODE_INTERVIEW_PROMPT_ENHANCER = "interview_prompt_enhancer";

    private static final String NODE_SOURCE_CODE_ADVISOR = "source_code_advisor";
    private static final String NODE_SOURCE_CODE_PROMPT_ENHANCER = "source_code_prompt_enhancer";

    private static final String NODE_CODE_READER = "code_reader";
    private static final String NODE_INTENT_CLASSIFIER = "intent_classifier";
    private static final String NODE_QA = "qa_node";
    private static final String NODE_MODIFICATION_PLANNER = "modification_planner";
    private static final String NODE_DATABASE_OPERATOR = "database_operator";
    private static final String NODE_CODE_MODIFIER = "code_modifier";

    private static final String NODE_BUILD_CHECK = "build_check";
    private static final String NODE_CODE_FIXER = "code_fixer";

    private static final String ROUTE_CREATE = "create";
    private static final String ROUTE_LEETCODE_CREATE = "leetcode_create";
    private static final String ROUTE_INTERVIEW_CREATE = "interview_create";
    private static final String ROUTE_INTERVIEW_SOURCE_CODE_CREATE = "interview_source_code_create";
    private static final String ROUTE_EXISTING_CODE = "existing_code";
    private static final String ROUTE_MODIFY = "modify";
    private static final String ROUTE_QA = "qa";
    private static final String ROUTE_EXECUTE_SQL = "execute_sql";
    private static final String ROUTE_SKIP_SQL = "skip_sql";
    private static final String ROUTE_FIX = "fix";
    private static final String ROUTE_PASS = "pass";

    private final ModeRouterNode modeRouterNode;
    private final ImagePlanNode imagePlanNode;
    private final ContentImageCollectorNode contentImageCollectorNode;
    private final IllustrationCollectorNode illustrationCollectorNode;
    private final DiagramCollectorNode diagramCollectorNode;
    private final LogoCollectorNode logoCollectorNode;
    private final ImageAggregatorNode imageAggregatorNode;
    private final PromptEnhancerNode promptEnhancerNode;
    private final CodeGeneratorNode codeGeneratorNode;
    private final LeetCodeAnimationAdvisorNode leetCodeAnimationAdvisorNode;
    private final LeetCodePromptEnhancerNode leetCodePromptEnhancerNode;
    private final InterviewAnimationAdvisorNode interviewAnimationAdvisorNode;
    private final InterviewPromptEnhancerNode interviewPromptEnhancerNode;
    private final SourceCodeAdvisorNode sourceCodeAdvisorNode;
    private final SourceCodePromptEnhancerNode sourceCodePromptEnhancerNode;
    private final CodeReaderNode codeReaderNode;
    private final IntentClassifierNode intentClassifierNode;
    private final ModificationPlannerNode modificationPlannerNode;
    private final DatabaseOperatorNode databaseOperatorNode;
    private final CodeModifierNode codeModifierNode;
    private final QANode qaNode;
    private final BuildCheckNode buildCheckNode;
    private final CodeFixerNode codeFixerNode;

    public CompiledGraph<MessagesState<String>> createWorkflow() throws GraphStateException {
        StateGraph<MessagesState<String>> createSubGraph = buildCreateModeSubGraph();
        StateGraph<MessagesState<String>> leetCodeCreateSubGraph = buildLeetCodeCreateSubGraph();
        StateGraph<MessagesState<String>> interviewCreateSubGraph = buildInterviewCreateSubGraph();
        StateGraph<MessagesState<String>> interviewSourceCodeCreateSubGraph = buildInterviewSourceCodeCreateSubGraph();
        StateGraph<MessagesState<String>> existingCodeSubGraph = buildExistingCodeSubGraph();
        StateGraph<MessagesState<String>> buildCheckSubGraph = buildBuildCheckSubGraph();

        return new MessagesStateGraph<String>()
                .addNode(NODE_MODE_ROUTER, modeRouterNode.action())
                .addNode(SUBGRAPH_CREATE, createSubGraph)
                .addNode(SUBGRAPH_LEETCODE_CREATE, leetCodeCreateSubGraph)
                .addNode(SUBGRAPH_INTERVIEW_CREATE, interviewCreateSubGraph)
                .addNode(SUBGRAPH_INTERVIEW_SOURCE_CODE_CREATE, interviewSourceCodeCreateSubGraph)
                .addNode(SUBGRAPH_EXISTING_CODE, existingCodeSubGraph)
                .addNode(SUBGRAPH_BUILD_CHECK, buildCheckSubGraph)
                .addEdge(START, NODE_MODE_ROUTER)
                .addConditionalEdges(NODE_MODE_ROUTER,
                        edge_async(modeRouterNode::routeToNextNode),
                        Map.of(
                                ROUTE_CREATE, SUBGRAPH_CREATE,
                                ROUTE_LEETCODE_CREATE, SUBGRAPH_LEETCODE_CREATE,
                                ROUTE_INTERVIEW_CREATE, SUBGRAPH_INTERVIEW_CREATE,
                                ROUTE_INTERVIEW_SOURCE_CODE_CREATE, SUBGRAPH_INTERVIEW_SOURCE_CODE_CREATE,
                                ROUTE_EXISTING_CODE, SUBGRAPH_EXISTING_CODE
                        ))
                .addEdge(SUBGRAPH_CREATE, SUBGRAPH_BUILD_CHECK)
                .addEdge(SUBGRAPH_LEETCODE_CREATE, SUBGRAPH_BUILD_CHECK)
                .addEdge(SUBGRAPH_INTERVIEW_CREATE, SUBGRAPH_BUILD_CHECK)
                .addEdge(SUBGRAPH_INTERVIEW_SOURCE_CODE_CREATE, SUBGRAPH_BUILD_CHECK)
                .addConditionalEdges(SUBGRAPH_EXISTING_CODE,
                        edge_async(this::routeAfterExistingCode),
                        Map.of(
                                ROUTE_MODIFY, SUBGRAPH_BUILD_CHECK,
                                ROUTE_QA, END
                        ))
                .addEdge(SUBGRAPH_BUILD_CHECK, END)
                .compile();
    }

    private StateGraph<MessagesState<String>> buildCreateModeSubGraph() throws GraphStateException {
        return new MessagesStateGraph<String>()
                .addNode(NODE_IMAGE_PLAN, imagePlanNode.action())
                .addNode(NODE_CONTENT_IMAGE_COLLECTOR, contentImageCollectorNode.action())
                .addNode(NODE_ILLUSTRATION_COLLECTOR, illustrationCollectorNode.action())
                .addNode(NODE_DIAGRAM_COLLECTOR, diagramCollectorNode.action())
                .addNode(NODE_LOGO_COLLECTOR, logoCollectorNode.action())
                .addNode(NODE_IMAGE_AGGREGATOR, imageAggregatorNode.action())
                .addNode(NODE_PROMPT_ENHANCER, promptEnhancerNode.action())
                .addNode(NODE_CODE_GENERATOR, codeGeneratorNode.action())
                .addEdge(START, NODE_IMAGE_PLAN)
                .addEdge(NODE_IMAGE_PLAN, NODE_CONTENT_IMAGE_COLLECTOR)
                .addEdge(NODE_IMAGE_PLAN, NODE_ILLUSTRATION_COLLECTOR)
                .addEdge(NODE_IMAGE_PLAN, NODE_DIAGRAM_COLLECTOR)
                .addEdge(NODE_IMAGE_PLAN, NODE_LOGO_COLLECTOR)
                .addEdge(NODE_CONTENT_IMAGE_COLLECTOR, NODE_IMAGE_AGGREGATOR)
                .addEdge(NODE_ILLUSTRATION_COLLECTOR, NODE_IMAGE_AGGREGATOR)
                .addEdge(NODE_DIAGRAM_COLLECTOR, NODE_IMAGE_AGGREGATOR)
                .addEdge(NODE_LOGO_COLLECTOR, NODE_IMAGE_AGGREGATOR)
                .addEdge(NODE_IMAGE_AGGREGATOR, NODE_PROMPT_ENHANCER)
                .addEdge(NODE_PROMPT_ENHANCER, NODE_CODE_GENERATOR)
                .addEdge(NODE_CODE_GENERATOR, END);
    }

    private StateGraph<MessagesState<String>> buildLeetCodeCreateSubGraph() throws GraphStateException {
        return new MessagesStateGraph<String>()
                .addNode(NODE_ANIMATION_ADVISOR, leetCodeAnimationAdvisorNode.action())
                .addNode(NODE_LEETCODE_PROMPT_ENHANCER, leetCodePromptEnhancerNode.action())
                .addNode(NODE_CODE_GENERATOR, codeGeneratorNode.action())
                .addEdge(START, NODE_ANIMATION_ADVISOR)
                .addEdge(NODE_ANIMATION_ADVISOR, NODE_LEETCODE_PROMPT_ENHANCER)
                .addEdge(NODE_LEETCODE_PROMPT_ENHANCER, NODE_CODE_GENERATOR)
                .addEdge(NODE_CODE_GENERATOR, END);
    }

    private StateGraph<MessagesState<String>> buildInterviewCreateSubGraph() throws GraphStateException {
        return new MessagesStateGraph<String>()
                .addNode(NODE_INTERVIEW_ANIMATION_ADVISOR, interviewAnimationAdvisorNode.action())
                .addNode(NODE_INTERVIEW_PROMPT_ENHANCER, interviewPromptEnhancerNode.action())
                .addNode(NODE_CODE_GENERATOR, codeGeneratorNode.action())
                .addEdge(START, NODE_INTERVIEW_ANIMATION_ADVISOR)
                .addEdge(NODE_INTERVIEW_ANIMATION_ADVISOR, NODE_INTERVIEW_PROMPT_ENHANCER)
                .addEdge(NODE_INTERVIEW_PROMPT_ENHANCER, NODE_CODE_GENERATOR)
                .addEdge(NODE_CODE_GENERATOR, END);
    }

    private StateGraph<MessagesState<String>> buildInterviewSourceCodeCreateSubGraph() throws GraphStateException {
        return new MessagesStateGraph<String>()
                .addNode(NODE_SOURCE_CODE_ADVISOR, sourceCodeAdvisorNode.action())
                .addNode(NODE_SOURCE_CODE_PROMPT_ENHANCER, sourceCodePromptEnhancerNode.action())
                .addNode(NODE_CODE_GENERATOR, codeGeneratorNode.action())
                .addEdge(START, NODE_SOURCE_CODE_ADVISOR)
                .addEdge(NODE_SOURCE_CODE_ADVISOR, NODE_SOURCE_CODE_PROMPT_ENHANCER)
                .addEdge(NODE_SOURCE_CODE_PROMPT_ENHANCER, NODE_CODE_GENERATOR)
                .addEdge(NODE_CODE_GENERATOR, END);
    }

    private StateGraph<MessagesState<String>> buildExistingCodeSubGraph() throws GraphStateException {
        return new MessagesStateGraph<String>()
                .addNode(NODE_CODE_READER, codeReaderNode.action())
                .addNode(NODE_INTENT_CLASSIFIER, intentClassifierNode.action())
                .addNode(NODE_MODIFICATION_PLANNER, modificationPlannerNode.action())
                .addNode(NODE_DATABASE_OPERATOR, databaseOperatorNode.action())
                .addNode(NODE_CODE_MODIFIER, codeModifierNode.action())
                .addNode(NODE_QA, qaNode.action())
                .addEdge(START, NODE_CODE_READER)
                .addEdge(NODE_CODE_READER, NODE_INTENT_CLASSIFIER)
                .addConditionalEdges(NODE_INTENT_CLASSIFIER,
                        edge_async(intentClassifierNode::routeByIntent),
                        Map.of(
                                ROUTE_MODIFY, NODE_MODIFICATION_PLANNER,
                                ROUTE_QA, NODE_QA
                        ))
                .addConditionalEdges(NODE_MODIFICATION_PLANNER,
                        edge_async(this::routeAfterModificationPlanning),
                        Map.of(
                                ROUTE_EXECUTE_SQL, NODE_DATABASE_OPERATOR,
                                ROUTE_SKIP_SQL, NODE_CODE_MODIFIER
                        ))
                .addEdge(NODE_DATABASE_OPERATOR, NODE_CODE_MODIFIER)
                .addEdge(NODE_CODE_MODIFIER, END)
                .addEdge(NODE_QA, END);
    }

    private StateGraph<MessagesState<String>> buildBuildCheckSubGraph() throws GraphStateException {
        return new MessagesStateGraph<String>()
                .addNode(NODE_BUILD_CHECK, buildCheckNode.action())
                .addNode(NODE_CODE_FIXER, codeFixerNode.action())
                .addEdge(START, NODE_BUILD_CHECK)
                .addConditionalEdges(NODE_BUILD_CHECK,
                        edge_async(this::routeInBuildCheck),
                        Map.of(
                                ROUTE_FIX, NODE_CODE_FIXER,
                                ROUTE_PASS, END
                        ))
                .addEdge(NODE_CODE_FIXER, NODE_BUILD_CHECK);
    }

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

    private String routeInBuildCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();

        if (qualityResult == null || !qualityResult.getIsValid()) {
            if (context.getFixRetryCount() < WorkflowContext.MAX_FIX_RETRY_COUNT) {
                log.info("构建未通过，路由到修复节点 (重试次数: {}/{})",
                        context.getFixRetryCount(), WorkflowContext.MAX_FIX_RETRY_COUNT);
                return ROUTE_FIX;
            }
            log.warn("达到最大修复重试次数 ({})，强制通过构建检查", WorkflowContext.MAX_FIX_RETRY_COUNT);
        }

        log.info("构建通过或达到最大重试次数，路由到出口");
        return ROUTE_PASS;
    }
}
