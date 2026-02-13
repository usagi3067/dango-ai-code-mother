package com.dango.supabase;

import com.dango.supabase.dto.SupabaseConfigDTO;
import com.dango.supabase.dto.TableSchemaDTO;
import com.dango.supabase.dto.TableSummaryDTO;
import com.dango.supabase.service.SupabaseService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Supabase 服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SupabaseServiceTest {

    @Autowired
    private SupabaseService supabaseService;

    // 测试用的 appId
    private static final Long TEST_APP_ID = 999999L;

    @Test
    @Order(1)
    void testCreateSchema() {
        // 创建 Schema
        SupabaseConfigDTO config = supabaseService.createSchema(TEST_APP_ID);

        assertNotNull(config);
        assertEquals("app_" + TEST_APP_ID, config.getSchemaName());
        assertNotNull(config.getUrl());
        assertNotNull(config.getAnonKey());

        System.out.println("Schema 创建成功: " + config);
    }

    @Test
    @Order(2)
    void testExecuteSql_CreateTable() {
        // 创建测试表
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS test_users (
                id SERIAL PRIMARY KEY,
                name TEXT NOT NULL,
                email TEXT,
                created_at TIMESTAMP DEFAULT NOW()
            )
            """;

        String result = supabaseService.executeSql(TEST_APP_ID, createTableSql);
        System.out.println("创建表结果: " + result);

        // 授权 anon 访问
        String grantSql = "GRANT SELECT, INSERT, UPDATE, DELETE ON test_users TO anon";
        supabaseService.executeSql(TEST_APP_ID, grantSql);
    }

    @Test
    @Order(3)
    void testExecuteSql_InsertData() {
        // 插入测试数据
        String insertSql = """
            INSERT INTO test_users (name, email) VALUES
            ('张三', 'zhangsan@test.com'),
            ('李四', 'lisi@test.com')
            RETURNING *
            """;

        String result = supabaseService.executeSql(TEST_APP_ID, insertSql);
        System.out.println("插入数据结果: " + result);

        assertTrue(result.contains("张三"));
        assertTrue(result.contains("李四"));
    }

    @Test
    @Order(4)
    void testExecuteSql_SelectData() {
        // 查询数据
        String selectSql = "SELECT * FROM test_users";
        String result = supabaseService.executeSql(TEST_APP_ID, selectSql);

        System.out.println("查询结果: " + result);
        assertTrue(result.contains("张三"));
    }

    @Test
    @Order(5)
    void testGetSchema() {
        // 获取表结构
        List<TableSchemaDTO> schemas = supabaseService.getSchema(TEST_APP_ID);

        assertNotNull(schemas);
        assertFalse(schemas.isEmpty());

        System.out.println("表结构:");
        schemas.forEach(s -> System.out.println("  " + s.getTableName() + "." + s.getColumnName() + " - " + s.getDataType()));
    }

    @Test
    @Order(6)
    void testGetTableSummary() {
        // 获取表摘要
        List<TableSummaryDTO> summaries = supabaseService.getTableSummary(TEST_APP_ID);

        assertNotNull(summaries);
        assertFalse(summaries.isEmpty());

        System.out.println("表摘要:");
        summaries.forEach(s -> System.out.println("  " + s.getTableName() + ": " + s.getRowCount() + " 行"));
    }

    @Test
    @Order(7)
    void testGetSupabaseConfig() {
        // 获取配置
        SupabaseConfigDTO config = supabaseService.getSupabaseConfig(TEST_APP_ID);

        assertNotNull(config);
        assertEquals("app_" + TEST_APP_ID, config.getSchemaName());

        System.out.println("Supabase 配置: " + config);
    }

    @Test
    @Order(8)
    void testDeleteSchema() {
        // 删除 Schema（清理测试数据）
        supabaseService.deleteSchema(TEST_APP_ID);

        System.out.println("Schema 删除成功");

        // 验证删除后获取表结构为空
        List<TableSchemaDTO> schemas = supabaseService.getSchema(TEST_APP_ID);
        assertTrue(schemas.isEmpty());
    }
}
