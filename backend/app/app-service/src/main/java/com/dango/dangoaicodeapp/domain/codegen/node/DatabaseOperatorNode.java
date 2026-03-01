package com.dango.dangoaicodeapp.domain.codegen.node;

import com.dango.dangoaicodeapp.domain.codegen.port.DatabaseOperationPort;
import com.dango.dangoaicodeapp.domain.codegen.port.WorkflowMessagePort;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.SqlExecutionResult;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.SqlStatement;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 数据库操作节点。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseOperatorNode {

    private static final String NODE_NAME = "数据库操作";

    private final WorkflowMessagePort workflowMessagePort;
    private final DatabaseOperationPort databaseOperationPort;

    public AsyncNodeAction<MessagesState<String>> action() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: {}", NODE_NAME);

            workflowMessagePort.emitNodeStart(context.getWorkflowExecutionId(), NODE_NAME);

            List<SqlStatement> statements = context.getPlannedSqlStatements();
            List<SqlExecutionResult> results = new ArrayList<>();

            if (statements == null || statements.isEmpty()) {
                log.info("没有需要执行的 SQL 语句");
                workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, "没有需要执行的 SQL 语句\n");
                context.setExecutionResults(results);
                workflowMessagePort.emitNodeComplete(context.getWorkflowExecutionId(), NODE_NAME);
                context.setCurrentStep(NODE_NAME);
                return WorkflowContext.saveContext(context);
            }

            workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME,
                    String.format("开始执行 %d 条 SQL 语句...\n", statements.size()));

            int successCount = 0;
            int failCount = 0;

            for (int i = 0; i < statements.size(); i++) {
                SqlStatement stmt = statements.get(i);
                workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME,
                        String.format("  [%d/%d] %s...", i + 1, statements.size(), stmt.getDescription()));

                SqlExecutionResult result = executeSql(context.getAppId(), stmt);
                results.add(result);

                if (result.isSuccess()) {
                    successCount++;
                    workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, " ✓\n");
                } else {
                    failCount++;
                    workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME, " ✗\n");
                    workflowMessagePort.emitNodeError(context.getWorkflowExecutionId(), NODE_NAME,
                            "SQL 执行失败: " + stmt.getDescription() + " - " + result.getError());
                }
            }

            context.setExecutionResults(results);

            try {
                context.setLatestDatabaseSchema(databaseOperationPort.getFormattedSchema(context.getAppId()));
            } catch (Exception e) {
                log.error("获取最新 Schema 失败: {}", e.getMessage(), e);
                context.setLatestDatabaseSchema(context.getDatabaseSchema());
            }

            workflowMessagePort.emitNodeMessage(context.getWorkflowExecutionId(), NODE_NAME,
                    String.format("\n执行完成：成功 %d 条，失败 %d 条\n", successCount, failCount));

            workflowMessagePort.emitNodeComplete(context.getWorkflowExecutionId(), NODE_NAME);
            context.setCurrentStep(NODE_NAME);
            return WorkflowContext.saveContext(context);
        });
    }

    private SqlExecutionResult executeSql(Long appId, SqlStatement stmt) {
        try {
            String result = databaseOperationPort.executeSql(appId, stmt.getSql());
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
}
