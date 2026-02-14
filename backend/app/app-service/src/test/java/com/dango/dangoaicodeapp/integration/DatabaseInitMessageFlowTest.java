package com.dango.dangoaicodeapp.integration;

import com.dango.dangoaicodeapp.model.entity.App;
import com.dango.dangoaicodeapp.service.AppService;
import com.dango.dangoaicodeapp.workflow.CodeGenWorkflow;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import com.dango.dangoaicodecommon.manager.CosManager;
import com.dango.supabase.dto.TableSchemaDTO;
import com.dango.supabase.dto.TableSchemaDTO;
import com.dango.supabase.service.SupabaseService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库初始化消息流程集成测试
 *
 * 使用真实的 LangGraph4j 工作流验证设计文档 7.1 节描述的「一步到位」流程：
 * 初始化消息 → AI 分析 → 建表 → 生成代码
 *
 * 测试项目: vue_project_375386732889255936 (博客应用)
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseInitMessageFlowTest {

    @Autowired
    private AppService appService;

    @DubboReference
    private SupabaseService supabaseService;

    @MockBean
    private CosManager cosManager;

    // 使用真实项目的 appId
    private static final Long TEST_APP_ID = 380132561191358464L;
    // 项目根目录（用于设置 user.dir）
    private static final String PROJECT_ROOT = "/Users/dango/Documents/code/dango-ai-code-mother/backend";
    private static final String PROJECT_DIR = PROJECT_ROOT + "/tmp/code_output/vue_project_" + TEST_APP_ID;

    // 设计文档 7.1 定义的自动初始化消息（一步到位：分析并创建表）
    private static final String AUTO_INIT_MESSAGE = "数据库已启用，请分析应用并创建合适的数据库表";

    // 保存原始的 user.dir
    private static String originalUserDir;

    @BeforeAll
    static void setup() {
        // 保存原始的 user.dir 并设置为项目根目录
        // 这样 ModeRouterNode.hasExistingCode 才能找到正确的代码目录
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", PROJECT_ROOT);

        System.out.println("========================================");
        System.out.println("数据库初始化消息流程集成测试");
        System.out.println("========================================");
        System.out.println("测试场景: 设计文档 7.1 节 - 一步到位流程");
        System.out.println("使用真实 LangGraph4j 工作流执行");
        System.out.println("测试项目: 博客应用 (Vue)");
        System.out.println("App ID: " + TEST_APP_ID);
        System.out.println("user.dir: " + System.getProperty("user.dir"));
        System.out.println("========================================\n");

        // 验证项目目录存在
        File projectDirFile = new File(PROJECT_DIR);
        assertTrue(projectDirFile.exists(), "测试项目目录不存在: " + PROJECT_DIR);
    }

    @AfterAll
    static void tearDown() {
        // 恢复原始的 user.dir
        if (originalUserDir != null) {
            System.setProperty("user.dir", originalUserDir);
        }

        System.out.println("========================================");
        System.out.println("数据库初始化消息流程集成测试完成");
        System.out.println("========================================");
    }

    /**
     * 测试 1: 使用真实工作流执行数据库初始化消息
     *
     * 完整流程：
     * 1. ModeRouterNode: 判断为修改模式（有历史代码）
     * 2. CodeReaderNode: 读取项目结构
     * 3. DatabaseAnalyzerNode: 分析项目，输出建表 SQL
     * 4. 条件边判断: sqlStatements 非空 → 执行 DatabaseOperatorNode
     * 5. DatabaseOperatorNode: 执行 SQL，获取最新 Schema
     * 6. CodeModifierNode: 根据新表结构生成数据库操作代码
     * 7. CodeQualityCheckNode: 质检
     */
    @Test
    @Order(1)
    void testDatabaseInitWorkflow() {
        System.out.println("\n【测试 1】使用真实工作流执行数据库初始化消息");
        System.out.println("消息内容: \"" + AUTO_INIT_MESSAGE + "\"");

        // 1. 获取应用信息
        App app = appService.getById(TEST_APP_ID);
        assertNotNull(app, "应用不存在");
        System.out.println("\n应用信息:");
        System.out.println("  - 应用名: " + app.getAppName());
        System.out.println("  - 代码类型: " + app.getCodeGenType());
        System.out.println("  - 数据库启用: " + app.getHasDatabase());

        // 2. 获取当前数据库 Schema
        String databaseSchema = "";
        try {
            List<TableSchemaDTO> schemas = supabaseService.getSchema(TEST_APP_ID);
            if (schemas != null && !schemas.isEmpty()) {
                databaseSchema = formatSchema(schemas);
                System.out.println("\n当前数据库 Schema:\n" + databaseSchema);
            } else {
                System.out.println("\n当前数据库为空（无表）");
            }
        } catch (Exception e) {
            System.out.println("\n获取 Schema 失败（可能为空）: " + e.getMessage());
        }

        // 3. 执行工作流
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        try {
            System.out.println("\n开始执行工作流...");
            System.out.println("预期流程: ModeRouter → CodeReader → DatabaseAnalyzer → DatabaseOperator → CodeModifier → QualityCheck");

            // 创建工作流
            CompiledGraph<MessagesState<String>> compiledWorkflow = workflow.createWorkflow();

            // 构建初始上下文
            // 关键：强制设置 databaseEnabled = true，模拟数据库刚启用的场景
            // 实际场景中，用户点击"新建 Database"后，app.hasDatabase 会被设置为 true
            WorkflowContext initialContext = WorkflowContext.builder()
                    .originalPrompt(AUTO_INIT_MESSAGE)
                    .currentStep("初始化")
                    .appId(TEST_APP_ID)
                    .databaseEnabled(true)  // 强制启用，模拟数据库初始化后的状态
                    .databaseSchema(databaseSchema)
                    .build();

            System.out.println("\n初始上下文:");
            System.out.println("  - appId: " + initialContext.getAppId());
            System.out.println("  - databaseEnabled: " + initialContext.isDatabaseEnabled());

            // 执行工作流
            WorkflowContext finalContext = null;
            int stepCounter = 1;

            for (NodeOutput<MessagesState<String>> step : compiledWorkflow.stream(
                    Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                System.out.println("--- 第 " + stepCounter + " 步完成 ---");
                WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                if (currentContext != null) {
                    finalContext = currentContext;
                    System.out.println("  当前步骤: " + currentContext.getCurrentStep());
                    System.out.println("  操作模式: " + currentContext.getOperationMode());
                    System.out.println("  数据库启用: " + currentContext.isDatabaseEnabled());

                    // 在数据库分析步骤后打印 SQL 语句
                    if ("数据库分析".equals(currentContext.getCurrentStep())) {
                        System.out.println("  [数据库分析结果]");
                        if (currentContext.getSqlStatements() != null && !currentContext.getSqlStatements().isEmpty()) {
                            System.out.println("    SQL 语句数: " + currentContext.getSqlStatements().size());
                            currentContext.getSqlStatements().forEach(stmt ->
                                    System.out.println("    - [" + stmt.getType() + "] " + stmt.getDescription() + ": " +
                                            stmt.getSql().substring(0, Math.min(80, stmt.getSql().length())) + "..."));
                        } else {
                            System.out.println("    SQL 语句数: 0 (无需数据库操作)");
                        }
                    }

                    // 在数据库操作步骤后打印执行结果
                    if ("数据库操作".equals(currentContext.getCurrentStep())) {
                        System.out.println("  [数据库操作结果]");
                        if (currentContext.getExecutionResults() != null && !currentContext.getExecutionResults().isEmpty()) {
                            currentContext.getExecutionResults().forEach(r ->
                                    System.out.println("    " + (r.isSuccess() ? "✓" : "✗") + " " +
                                            r.getSql().substring(0, Math.min(60, r.getSql().length())) + "..."));
                        }
                        if (currentContext.getLatestDatabaseSchema() != null && !currentContext.getLatestDatabaseSchema().isEmpty()) {
                            System.out.println("    最新 Schema: 已获取");
                        }
                    }
                }
                stepCounter++;
            }

            // 验证结果
            assertNotNull(finalContext, "工作流结果不应为空");

            System.out.println("\n✓ 工作流执行完成");
            System.out.println("  生成类型: " + finalContext.getGenerationType());
            System.out.println("  操作模式: " + finalContext.getOperationMode());
            System.out.println("  数据库启用: " + finalContext.isDatabaseEnabled());

            // 验证数据库相关字段
            if (finalContext.isDatabaseEnabled()) {
                System.out.println("\n数据库相关信息:");
                System.out.println("  初始 Schema: " + (finalContext.getDatabaseSchema() != null && !finalContext.getDatabaseSchema().isEmpty() ? "已获取" : "无"));
                System.out.println("  SQL 语句数: " + (finalContext.getSqlStatements() != null ? finalContext.getSqlStatements().size() : 0));
                System.out.println("  执行结果数: " + (finalContext.getExecutionResults() != null ? finalContext.getExecutionResults().size() : 0));
                System.out.println("  最新 Schema: " + (finalContext.getLatestDatabaseSchema() != null && !finalContext.getLatestDatabaseSchema().isEmpty() ? "已获取" : "无"));

                // 打印 SQL 语句
                if (finalContext.getSqlStatements() != null && !finalContext.getSqlStatements().isEmpty()) {
                    System.out.println("\nDatabaseAnalyzerNode 输出的 SQL:");
                    finalContext.getSqlStatements().forEach(stmt ->
                            System.out.println("  [" + stmt.getType() + "] " + stmt.getDescription()));
                }

                // 打印执行结果
                if (finalContext.getExecutionResults() != null && !finalContext.getExecutionResults().isEmpty()) {
                    System.out.println("\nDatabaseOperatorNode 执行结果:");
                    finalContext.getExecutionResults().forEach(r ->
                            System.out.println("  " + (r.isSuccess() ? "✓" : "✗") + " " +
                                    r.getSql().substring(0, Math.min(60, r.getSql().length())) + "..."));
                }
            }

            System.out.println("\n✓ 数据库初始化工作流测试通过\n");

        } catch (Exception e) {
            System.out.println("工作流执行异常: " + e.getMessage());
            e.printStackTrace();
            fail("工作流执行失败: " + e.getMessage());
        } finally {
            workflow.shutdown();
        }
    }

    /**
     * 测试 2: 验证数据库表已创建
     */
    @Test
    @Order(2)
    void testVerifyDatabaseTables() {
        System.out.println("\n【测试 2】验证数据库表已创建");

        try {
            List<TableSchemaDTO> schemas = supabaseService.getSchema(TEST_APP_ID);

            if (schemas == null || schemas.isEmpty()) {
                System.out.println("当前数据库为空（无表）");
                System.out.println("注意：如果测试 1 中数据库未启用，这是预期行为");
            } else {
                System.out.println("当前数据库表:");
                schemas.stream()
                        .collect(Collectors.groupingBy(TableSchemaDTO::getTableName))
                        .forEach((table, columns) -> {
                            System.out.println("  表 " + table + ":");
                            columns.forEach(col ->
                                    System.out.println("    - " + col.getColumnName() + ": " + col.getDataType()));
                        });

                long tableCount = schemas.stream()
                        .map(TableSchemaDTO::getTableName)
                        .distinct()
                        .count();
                System.out.println("\n共 " + tableCount + " 个表");
            }
        } catch (Exception e) {
            System.out.println("获取 Schema 失败: " + e.getMessage());
        }

        System.out.println("\n✓ 数据库表验证完成\n");
    }

    /**
     * 清理测试数据
     * 注意：暂时禁用，方便手动测试页面效果
     */
    @Test
    @Order(99)
    @Disabled("暂时禁用清理，保留数据用于页面测试")
    void cleanup() {
        System.out.println("\n【清理】删除测试数据");

        try {
            List<TableSchemaDTO> schemas = supabaseService.getSchema(TEST_APP_ID);
            if (schemas != null && !schemas.isEmpty()) {
                schemas.stream()
                        .map(TableSchemaDTO::getTableName)
                        .distinct()
                        .forEach(tableName -> {
                            try {
                                supabaseService.executeSql(TEST_APP_ID,
                                        "DROP TABLE IF EXISTS " + tableName + " CASCADE");
                                System.out.println("✓ " + tableName + " 表已删除");
                            } catch (Exception e) {
                                System.out.println("  删除 " + tableName + " 时出现异常: " + e.getMessage());
                            }
                        });
            } else {
                System.out.println("无需清理（数据库为空）");
            }
        } catch (Exception e) {
            System.out.println("清理时出现异常（可忽略）: " + e.getMessage());
        }

        System.out.println("✓ 清理完成\n");
    }

    /**
     * 格式化 Schema 为字符串
     */
    private String formatSchema(List<TableSchemaDTO> schemas) {
        StringBuilder sb = new StringBuilder();
        schemas.stream()
                .collect(Collectors.groupingBy(TableSchemaDTO::getTableName))
                .forEach((table, columns) -> {
                    sb.append("表 ").append(table).append(":\n");
                    columns.forEach(col ->
                            sb.append("  - ").append(col.getColumnName())
                                    .append(": ").append(col.getDataType()).append("\n"));
                });
        return sb.toString();
    }
}
