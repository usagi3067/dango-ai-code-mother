package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.domain.codegen.workflow.state.SqlExecutionResult;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.SqlStatement;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import com.dango.supabase.dto.TableSchemaDTO;
import com.dango.supabase.service.SupabaseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 数据库操作节点
 * 批量执行 SQL 语句，获取最新 Schema
 *
 * 职责：
 * - 批量执行 sqlStatements 中的 SQL
 * - 记录执行结果到 executionResults
 * - 执行完成后获取最新表结构到 latestDatabaseSchema
 *
 * 特点：
 * - 无 AI 调用，纯粹的 SQL 执行逻辑
 * - 通过 Dubbo 调用 supabase-service
 *
 * @author dango
 */
@Slf4j
@Component
public class DatabaseOperatorNode {

    private static final String NODE_NAME = "数据库操作";

    @DubboReference
    private SupabaseService supabaseService;

    /**
     * 创建节点动作
     * 注意：由于需要注入 Dubbo 服务，这里使用实例方法而非静态方法
     */
    public AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            // 发送节点开始消息
            context.emitNodeStart(NODE_NAME);

            // 从 ModificationPlanner 的规划结果中获取 SQL 列表
            List<SqlStatement> statements = context.getPlannedSqlStatements();
            List<SqlExecutionResult> results = new ArrayList<>();

            if (statements == null || statements.isEmpty()) {
                log.info("没有需要执行的 SQL 语句");
                context.emitNodeMessage(NODE_NAME, "没有需要执行的 SQL 语句\n");
                context.setExecutionResults(results);
                context.emitNodeComplete(NODE_NAME);
                context.setCurrentStep(NODE_NAME);
                return WorkflowContext.saveContext(context);
            }

            context.emitNodeMessage(NODE_NAME,
                    String.format("开始执行 %d 条 SQL 语句...\n", statements.size()));

            // 批量执行 SQL
            int successCount = 0;
            int failCount = 0;

            for (int i = 0; i < statements.size(); i++) {
                SqlStatement stmt = statements.get(i);
                context.emitNodeMessage(NODE_NAME,
                        String.format("  [%d/%d] %s...", i + 1, statements.size(), stmt.getDescription()));

                SqlExecutionResult result = executeSql(context.getAppId(), stmt);
                results.add(result);

                if (result.isSuccess()) {
                    successCount++;
                    context.emitNodeMessage(NODE_NAME, " ✓\n");
                } else {
                    failCount++;
                    context.emitNodeMessage(NODE_NAME, " ✗\n");
                    context.emitNodeError(NODE_NAME,
                            "SQL 执行失败: " + stmt.getDescription() + " - " + result.getError());
                }
            }

            context.setExecutionResults(results);

            // 获取执行后的最新 Schema
            try {
                List<TableSchemaDTO> latestSchema = supabaseService.getSchema(context.getAppId());
                String schemaStr = formatSchema(latestSchema);
                context.setLatestDatabaseSchema(schemaStr);
                log.info("获取最新 Schema 成功:\n{}", schemaStr);
            } catch (Exception e) {
                log.error("获取最新 Schema 失败: {}", e.getMessage(), e);
                // 失败时使用原始 Schema
                context.setLatestDatabaseSchema(context.getDatabaseSchema());
            }

            // 输出执行摘要
            context.emitNodeMessage(NODE_NAME,
                    String.format("\n执行完成：成功 %d 条，失败 %d 条\n", successCount, failCount));

            // 发送节点完成消息
            context.emitNodeComplete(NODE_NAME);

            // 更新状态
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 执行单条 SQL
     */
    private SqlExecutionResult executeSql(Long appId, SqlStatement stmt) {
        try {
            String result = supabaseService.executeSql(appId, stmt.getSql());
            return SqlExecutionResult.builder()
                    .sql(stmt.getSql())
                    .success(true)
                    .result(result)
                    .build();
        } catch (Exception e) {
            log.error("SQL 执行失败: {}", e.getMessage(), e);
            return SqlExecutionResult.builder()
                    .sql(stmt.getSql())
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * 格式化 Schema 信息为字符串
     */
    private String formatSchema(List<TableSchemaDTO> schemas) {
        if (schemas == null || schemas.isEmpty()) {
            return "无表";
        }

        // 按表名分组
        return schemas.stream()
                .collect(Collectors.groupingBy(TableSchemaDTO::getTableName))
                .entrySet().stream()
                .map(entry -> {
                    String tableName = entry.getKey();
                    List<TableSchemaDTO> columns = entry.getValue();
                    String columnStr = columns.stream()
                            .map(col -> String.format("  - %s: %s%s",
                                    col.getColumnName(),
                                    col.getDataType(),
                                    col.getIsNullable() ? "" : " NOT NULL"))
                            .collect(Collectors.joining("\n"));
                    return String.format("表 %s:\n%s", tableName, columnStr);
                })
                .collect(Collectors.joining("\n\n"));
    }
}
