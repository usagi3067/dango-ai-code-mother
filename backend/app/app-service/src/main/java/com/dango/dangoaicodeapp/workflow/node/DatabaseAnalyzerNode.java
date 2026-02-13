package com.dango.dangoaicodeapp.workflow.node;

import cn.hutool.core.util.StrUtil;
import com.dango.aicodegenerate.model.DatabaseAnalysisResult;
import com.dango.aicodegenerate.service.AiDatabaseAnalyzerService;
import com.dango.dangoaicodeapp.ai.AiDatabaseAnalyzerServiceFactory;
import com.dango.dangoaicodeapp.workflow.state.SqlStatement;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 数据库分析节点
 * 分析用户需求，判断是否需要数据库操作，输出 SQL 语句列表
 *
 * 职责：
 * - 接收项目结构、数据库 Schema、用户需求
 * - 调用 AI 分析是否需要数据库操作
 * - 输出 sqlStatements 列表（可能为空）
 *
 * @author dango
 */
@Slf4j
@Component
public class DatabaseAnalyzerNode {

    private static final String NODE_NAME = "数据库分析";

    /**
     * 创建节点动作
     */
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            // 如果未启用数据库，直接返回空列表
            if (!context.isDatabaseEnabled()) {
                log.info("数据库未启用，跳过分析");
                context.setSqlStatements(Collections.emptyList());
                context.setCurrentStep(NODE_NAME);
                return WorkflowContext.saveContext(context);
            }

            // 发送节点开始消息
            context.emitNodeStart(NODE_NAME);
            context.emitNodeMessage(NODE_NAME, "正在分析数据库操作需求...\n");

            try {
                // 恢复监控上下文
                context.restoreMonitorContext();

                // 构建分析请求
                String analysisRequest = buildAnalysisRequest(context);
                log.info("数据库分析请求:\n{}", analysisRequest);

                // 获取 AI 分析服务
                AiDatabaseAnalyzerServiceFactory analyzerFactory = SpringContextUtil.getBean(AiDatabaseAnalyzerServiceFactory.class);
                AiDatabaseAnalyzerService analyzerService = analyzerFactory.createAnalyzerService(context.getAppId());

                // 调用 AI 分析（结构化输出）
                DatabaseAnalysisResult analysisResult = analyzerService.analyze(context.getAppId(), analysisRequest);
                log.info("数据库分析结果: {}", analysisResult);

                // 转换为 WorkflowContext 使用的 SqlStatement 列表
                List<SqlStatement> sqlStatements = convertToSqlStatements(analysisResult);
                context.setSqlStatements(sqlStatements);

                if (sqlStatements.isEmpty()) {
                    context.emitNodeMessage(NODE_NAME, "分析完成：无需数据库操作\n");
                    if (analysisResult != null && StrUtil.isNotBlank(analysisResult.getAnalysis())) {
                        context.emitNodeMessage(NODE_NAME, "原因：" + analysisResult.getAnalysis() + "\n");
                    }
                } else {
                    context.emitNodeMessage(NODE_NAME,
                            String.format("分析完成：需要执行 %d 条 SQL 语句\n", sqlStatements.size()));
                    for (SqlStatement stmt : sqlStatements) {
                        context.emitNodeMessage(NODE_NAME,
                                String.format("  - [%s] %s\n", stmt.getType(), stmt.getDescription()));
                    }
                }

            } catch (Exception e) {
                log.error("数据库分析失败: {}", e.getMessage(), e);
                context.setSqlStatements(Collections.emptyList());
                context.emitNodeError(NODE_NAME, e.getMessage());
            } finally {
                context.clearMonitorContext();
            }

            // 发送节点完成消息
            context.emitNodeComplete(NODE_NAME);

            // 更新状态
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 构建分析请求
     */
    private static String buildAnalysisRequest(WorkflowContext context) {
        StringBuilder request = new StringBuilder();

        // 数据库信息
        request.append("## 当前数据库状态\n");
        request.append("Schema: app_").append(context.getAppId()).append("\n");

        String databaseSchema = context.getDatabaseSchema();
        if (StrUtil.isNotBlank(databaseSchema)) {
            request.append("现有表结构:\n").append(databaseSchema).append("\n\n");
        } else {
            request.append("现有表结构: 无（空数据库）\n\n");
        }

        // 项目结构
        String projectStructure = context.getProjectStructure();
        if (StrUtil.isNotBlank(projectStructure)) {
            request.append("## 项目结构\n```\n")
                   .append(projectStructure)
                   .append("```\n\n");
        }

        // 用户需求
        String originalPrompt = context.getOriginalPrompt();
        if (StrUtil.isNotBlank(originalPrompt)) {
            request.append("## 用户需求\n")
                   .append(originalPrompt)
                   .append("\n\n");
        }

        return request.toString();
    }

    /**
     * 将 AI 分析结果转换为 SqlStatement 列表
     */
    private static List<SqlStatement> convertToSqlStatements(DatabaseAnalysisResult result) {
        if (result == null || result.getSqlStatements() == null || result.getSqlStatements().isEmpty()) {
            return Collections.emptyList();
        }

        return result.getSqlStatements().stream()
                .map(item -> SqlStatement.builder()
                        .type(item.getType())
                        .sql(item.getSql())
                        .description(item.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}
