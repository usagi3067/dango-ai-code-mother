package com.dango.dangoaicodeapp.workflow.node;

import com.dango.aicodegenerate.model.DatabaseAnalysisResult;
import com.dango.dangoaicodeapp.domain.codegen.ai.service.AiDatabaseAnalyzerService;
import com.dango.dangoaicodeapp.domain.codegen.ai.factory.AiDatabaseAnalyzerServiceFactory;
import com.dango.supabase.dto.TableSchemaDTO;
import com.dango.supabase.service.SupabaseService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库节点验收测试
 *
 * 基于真实 Vue 项目 (app_375386732889255936) 进行验收
 * 项目路径: /Users/dango/Documents/code/dango-ai-code-mother/backend/tmp/code_output/vue_project_375386732889255936
 *
 * 项目结构:
 * - src/App.vue - 主应用组件
 * - src/pages/Home.vue - 博客首页，展示文章列表
 * - src/pages/Article.vue - 文章详情页
 * - src/router/index.js - 路由配置
 *
 * 验收标准:
 * 任务 4 (DatabaseAnalyzerNode):
 * - 能正确分析功能开发类需求（输出 DDL）
 * - 能正确分析数据操作类需求（输出 DML）
 * - 能正确识别纯代码修改需求（输出空列表）
 *
 * 任务 5 (DatabaseOperatorNode):
 * - DDL 语句执行成功
 * - DML 语句执行成功
 * - 执行失败时正确记录错误
 * - 能获取执行后的最新 Schema
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseNodeAcceptanceTest {

    @Autowired
    private AiDatabaseAnalyzerServiceFactory analyzerServiceFactory;

    @DubboReference
    private SupabaseService supabaseService;

    // 使用真实项目的 appId
    private static final Long TEST_APP_ID = 375386732889255936L;

    // 项目结构（模拟 CodeReaderNode 读取的结果）
    private static final String PROJECT_STRUCTURE = """
        项目目录结构:
        - index.html
        - package.json
        - vite.config.js
        - src/
          - App.vue
          - main.js
          - pages/
            - Home.vue
            - Article.vue
          - router/
            - index.js
        """;

    @BeforeAll
    static void setup() {
        System.out.println("========================================");
        System.out.println("数据库节点验收测试");
        System.out.println("测试项目: 博客应用 (Vue)");
        System.out.println("App ID: " + TEST_APP_ID);
        System.out.println("========================================\n");
    }

    // ==================== 任务 4 验收：DatabaseAnalyzerNode ====================

    /**
     * 验收 4.1: 功能开发类需求 - 应输出 DDL
     * 场景：用户要求添加评论功能
     */
    @Test
    @Order(1)
    void testAnalyzer_FeatureDevelopment_ShouldOutputDDL() {
        System.out.println("\n【验收 4.1】功能开发类需求 - 添加评论功能");

        AiDatabaseAnalyzerService analyzerService = analyzerServiceFactory.createAnalyzerService(TEST_APP_ID);

        String request = """
            ## 当前数据库状态
            Schema: app_%d
            现有表结构: 无（空数据库）

            ## 项目结构
            %s

            ## 用户需求
            给博客文章添加评论功能，用户可以对文章发表评论
            """.formatted(TEST_APP_ID, PROJECT_STRUCTURE);

        DatabaseAnalysisResult result = analyzerService.analyze(TEST_APP_ID, request);

        // 验证
        assertNotNull(result, "分析结果不应为空");
        assertNotNull(result.getSqlStatements(), "SQL 列表不应为空");
        assertFalse(result.getSqlStatements().isEmpty(), "功能开发需求应输出 SQL");

        boolean hasDDL = result.getSqlStatements().stream()
                .anyMatch(stmt -> "DDL".equalsIgnoreCase(stmt.getType()));
        assertTrue(hasDDL, "功能开发需求应包含 DDL 语句（CREATE TABLE）");

        // 输出结果
        System.out.println("✓ 分析结果: " + result.getAnalysis());
        System.out.println("✓ SQL 语句:");
        result.getSqlStatements().forEach(stmt ->
                System.out.println("  [" + stmt.getType() + "] " + stmt.getDescription()));
        System.out.println("✓ 验收通过：正确输出 DDL\n");
    }

    /**
     * 验收 4.2: 数据操作类需求 - 应输出 DML
     * 场景：用户要求添加测试文章数据
     */
    @Test
    @Order(2)
    void testAnalyzer_DataOperation_ShouldOutputDML() {
        System.out.println("\n【验收 4.2】数据操作类需求 - 添加测试数据");

        AiDatabaseAnalyzerService analyzerService = analyzerServiceFactory.createAnalyzerService(TEST_APP_ID);

        String request = """
            ## 当前数据库状态
            Schema: app_%d
            现有表结构:
            表 articles:
              - id: integer NOT NULL
              - title: text NOT NULL
              - summary: text
              - content: text
              - created_at: timestamp

            ## 项目结构
            %s

            ## 用户需求
            往数据库里添加几篇测试文章
            """.formatted(TEST_APP_ID, PROJECT_STRUCTURE);

        DatabaseAnalysisResult result = analyzerService.analyze(TEST_APP_ID, request);

        // 验证
        assertNotNull(result, "分析结果不应为空");
        assertNotNull(result.getSqlStatements(), "SQL 列表不应为空");
        assertFalse(result.getSqlStatements().isEmpty(), "数据操作需求应输出 SQL");

        boolean hasDML = result.getSqlStatements().stream()
                .anyMatch(stmt -> "DML".equalsIgnoreCase(stmt.getType()));
        assertTrue(hasDML, "数据操作需求应包含 DML 语句（INSERT）");

        // 输出结果
        System.out.println("✓ 分析结果: " + result.getAnalysis());
        System.out.println("✓ SQL 语句:");
        result.getSqlStatements().forEach(stmt ->
                System.out.println("  [" + stmt.getType() + "] " + stmt.getDescription()));
        System.out.println("✓ 验收通过：正确输出 DML\n");
    }

    /**
     * 验收 4.3: 纯代码修改需求 - 应输出空列表
     * 场景：用户要求修改导航栏颜色
     */
    @Test
    @Order(3)
    void testAnalyzer_PureCodeModification_ShouldOutputEmptyList() {
        System.out.println("\n【验收 4.3】纯代码修改需求 - 修改样式");

        AiDatabaseAnalyzerService analyzerService = analyzerServiceFactory.createAnalyzerService(TEST_APP_ID);

        String request = """
            ## 当前数据库状态
            Schema: app_%d
            现有表结构:
            表 articles:
              - id: integer NOT NULL
              - title: text NOT NULL

            ## 项目结构
            %s

            ## 用户需求
            把导航栏的背景色从黑色改成蓝色
            """.formatted(TEST_APP_ID, PROJECT_STRUCTURE);

        DatabaseAnalysisResult result = analyzerService.analyze(TEST_APP_ID, request);

        // 验证
        assertNotNull(result, "分析结果不应为空");
        assertNotNull(result.getSqlStatements(), "SQL 列表不应为 null");
        assertTrue(result.getSqlStatements().isEmpty(), "纯代码修改需求应输出空列表");

        // 输出结果
        System.out.println("✓ 分析结果: " + result.getAnalysis());
        System.out.println("✓ SQL 列表为空: " + result.getSqlStatements().isEmpty());
        System.out.println("✓ 验收通过：正确输出空列表\n");
    }

    // ==================== 任务 5 验收：DatabaseOperatorNode ====================

    /**
     * 验收 5.0: 准备工作 - 创建 Schema
     */
    @Test
    @Order(10)
    void testOperator_PrepareSchema() {
        System.out.println("\n【验收 5.0】准备工作 - 创建 Schema");

        try {
            supabaseService.createSchema(TEST_APP_ID);
            System.out.println("✓ Schema 创建成功: app_" + TEST_APP_ID);
        } catch (Exception e) {
            System.out.println("✓ Schema 可能已存在: " + e.getMessage());
        }
    }

    /**
     * 验收 5.1: DDL 语句执行成功
     */
    @Test
    @Order(11)
    void testOperator_DDL_ShouldSucceed() {
        System.out.println("\n【验收 5.1】DDL 语句执行 - CREATE TABLE");

        String ddlSql = """
            CREATE TABLE IF NOT EXISTS comments (
                id SERIAL PRIMARY KEY,
                article_id INTEGER NOT NULL,
                author TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT NOW()
            )
            """;

        String result = supabaseService.executeSql(TEST_APP_ID, ddlSql);

        assertNotNull(result, "DDL 执行结果不应为空");
        System.out.println("✓ DDL 执行成功");
        System.out.println("✓ 验收通过：DDL 语句执行成功\n");
    }

    /**
     * 验收 5.2: DML 语句执行成功
     */
    @Test
    @Order(12)
    void testOperator_DML_ShouldSucceed() {
        System.out.println("\n【验收 5.2】DML 语句执行 - INSERT");

        String dmlSql = """
            INSERT INTO comments (article_id, author, content) VALUES
            (1, '张三', '写得很好，学到了很多！'),
            (1, '李四', '期待更多文章'),
            (2, '王五', '响应式原理讲得很清楚')
            RETURNING *
            """;

        String result = supabaseService.executeSql(TEST_APP_ID, dmlSql);

        assertNotNull(result, "DML 执行结果不应为空");
        assertTrue(result.contains("张三"), "INSERT 结果应包含插入的数据");
        System.out.println("✓ DML 执行成功");
        System.out.println("✓ 插入数据: " + result.substring(0, Math.min(200, result.length())) + "...");
        System.out.println("✓ 验收通过：DML 语句执行成功\n");
    }

    /**
     * 验收 5.3: 执行失败时正确记录错误
     */
    @Test
    @Order(13)
    void testOperator_InvalidSQL_ShouldFail() {
        System.out.println("\n【验收 5.3】错误处理 - 无效 SQL");

        String invalidSql = "SELECT * FROM non_existent_table_xyz_123";

        Exception exception = assertThrows(Exception.class, () -> {
            supabaseService.executeSql(TEST_APP_ID, invalidSql);
        });

        assertNotNull(exception.getMessage(), "错误信息不应为空");
        System.out.println("✓ 正确抛出异常: " + exception.getMessage());
        System.out.println("✓ 验收通过：执行失败时正确记录错误\n");
    }

    /**
     * 验收 5.4: 能获取执行后的最新 Schema
     */
    @Test
    @Order(14)
    void testOperator_GetLatestSchema_ShouldWork() {
        System.out.println("\n【验收 5.4】获取最新 Schema");

        List<TableSchemaDTO> schemas = supabaseService.getSchema(TEST_APP_ID);

        assertNotNull(schemas, "Schema 列表不应为空");
        assertFalse(schemas.isEmpty(), "Schema 列表不应为空");

        boolean hasCommentsTable = schemas.stream()
                .anyMatch(s -> "comments".equals(s.getTableName()));
        assertTrue(hasCommentsTable, "Schema 应包含 comments 表");

        System.out.println("✓ 获取 Schema 成功:");
        schemas.stream()
                .collect(java.util.stream.Collectors.groupingBy(TableSchemaDTO::getTableName))
                .forEach((table, columns) -> {
                    System.out.println("  表 " + table + ":");
                    columns.forEach(col ->
                            System.out.println("    - " + col.getColumnName() + ": " + col.getDataType()));
                });
        System.out.println("✓ 验收通过：能获取执行后的最新 Schema\n");
    }

    /**
     * 验收 5.5: 清理测试数据
     */
    @Test
    @Order(99)
    void testOperator_Cleanup() {
        System.out.println("\n【清理】删除测试数据");

        try {
            supabaseService.executeSql(TEST_APP_ID, "DROP TABLE IF EXISTS comments");
            System.out.println("✓ 测试表已删除");
        } catch (Exception e) {
            System.out.println("清理时出现异常（可忽略）: " + e.getMessage());
        }

        // 注意：不删除 Schema，因为这是真实项目可能会用到的
        System.out.println("✓ 清理完成\n");
    }

    @AfterAll
    static void summary() {
        System.out.println("========================================");
        System.out.println("验收测试完成");
        System.out.println("========================================");
    }
}
