package com.dango.dangoaicodeapp.integration;

import com.dango.dangoaicodeapp.config.SupabaseClientConfig;
import com.dango.dangoaicodeapp.domain.app.entity.App;
import com.dango.dangoaicodeapp.service.AppService;
import com.dango.dangoaicodecommon.manager.CosManager;
import com.dango.supabase.service.SupabaseService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库集成测试
 *
 * 测试场景：
 * 1. Schema 创建
 * 2. 客户端配置文件生成
 * 3. package.json 更新
 *
 * 前置条件：
 * - Nacos 运行中
 * - supabase-service 运行中
 * - 测试项目目录存在
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseIntegrationTest {

    @Autowired
    private AppService appService;

    @Autowired
    private SupabaseClientConfig supabaseClientConfig;

    @DubboReference
    private SupabaseService supabaseService;

    // Mock CosManager 以避免测试环境中的依赖问题
    @MockBean
    private CosManager cosManager;

    // 使用真实项目的 appId
    private static final Long TEST_APP_ID = 380282345463500800L;
    // 使用绝对路径
    private static final String PROJECT_DIR = "/Users/dango/Documents/code/dango-ai-code-mother/backend/tmp/code_output/vue_project_" + TEST_APP_ID;

    @BeforeAll
    static void setup() {
        System.out.println("========================================");
        System.out.println("数据库集成测试");
        System.out.println("测试项目: vue_project_" + TEST_APP_ID);
        System.out.println("项目目录: " + PROJECT_DIR);
        System.out.println("========================================\n");

        // 验证项目目录存在
        File projectDirFile = new File(PROJECT_DIR);
        assertTrue(projectDirFile.exists(), "测试项目目录不存在: " + PROJECT_DIR);
    }

    /**
     * 测试 1: 验证 Supabase 服务连接 - 创建 Schema
     */
    @Test
    @Order(1)
    void testSupabaseServiceConnection() {
        System.out.println("\n【测试 1】验证 Supabase 服务连接 - 创建 Schema");

        try {
            // 尝试创建 Schema（如果已存在会抛异常，这是正常的）
            supabaseService.createSchema(TEST_APP_ID);
            System.out.println("✓ Schema 创建成功: app_" + TEST_APP_ID);
        } catch (Exception e) {
            // Schema 可能已存在
            System.out.println("✓ Schema 可能已存在: " + e.getMessage());
        }

        // 验证可以获取 Schema
        var schemas = supabaseService.getSchema(TEST_APP_ID);
        assertNotNull(schemas, "应能获取 Schema 信息");
        System.out.println("✓ 获取 Schema 成功，表数量: " + schemas.size());
    }

    /**
     * 测试 2: 验证配置读取
     */
    @Test
    @Order(2)
    void testSupabaseClientConfigLoaded() {
        System.out.println("\n【测试 2】验证 Supabase 客户端配置读取");

        assertNotNull(supabaseClientConfig.getUrl(), "URL 不应为空");
        assertNotNull(supabaseClientConfig.getAnonKey(), "AnonKey 不应为空");

        System.out.println("✓ URL: " + supabaseClientConfig.getUrl());
        System.out.println("✓ AnonKey: " + supabaseClientConfig.getAnonKey().substring(0, 20) + "...");
    }

    /**
     * 测试 3: 手动写入客户端配置文件并验证
     */
    @Test
    @Order(3)
    void testWriteSupabaseClientConfig() throws Exception {
        System.out.println("\n【测试 3】写入 Supabase 客户端配置文件");

        // 创建目录
        String supabaseDir = PROJECT_DIR + "/src/integrations/supabase";
        File supabaseDirFile = new File(supabaseDir);
        if (!supabaseDirFile.exists()) {
            supabaseDirFile.mkdirs();
        }

        // 写入 client.js
        String clientContent = """
            import { createClient } from '@supabase/supabase-js';

            const SUPABASE_URL = "%s";
            const SUPABASE_ANON_KEY = "%s";

            // Schema 名称（每个应用独立的数据库空间）
            export const SCHEMA_NAME = "app_%d";

            // 创建 Supabase 客户端
            // 导入方式: import { supabase } from "@/integrations/supabase/client";
            export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
                db: {
                    schema: SCHEMA_NAME
                }
            });
            """.formatted(supabaseClientConfig.getUrl(), supabaseClientConfig.getAnonKey(), TEST_APP_ID);

        File clientFile = new File(supabaseDir, "client.js");
        Files.writeString(clientFile.toPath(), clientContent, StandardCharsets.UTF_8);

        // 验证文件存在
        assertTrue(clientFile.exists(), "client.js 应存在");

        // 验证内容
        String content = Files.readString(clientFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(content.contains("@supabase/supabase-js"), "应包含 supabase-js 导入");
        assertTrue(content.contains("app_" + TEST_APP_ID), "应包含正确的 Schema 名称");

        System.out.println("✓ client.js 写入成功: " + clientFile.getAbsolutePath());
        System.out.println("✓ 包含正确的 Schema 名称: app_" + TEST_APP_ID);
    }

    /**
     * 测试 4: 更新 package.json
     */
    @Test
    @Order(4)
    void testUpdatePackageJson() throws Exception {
        System.out.println("\n【测试 4】更新 package.json");

        File packageJsonFile = new File(PROJECT_DIR, "package.json");
        assertTrue(packageJsonFile.exists(), "package.json 应存在");

        String content = Files.readString(packageJsonFile.toPath(), StandardCharsets.UTF_8);

        // 如果还没有 supabase 依赖，添加它
        if (!content.contains("@supabase/supabase-js")) {
            content = content.replace(
                    "\"dependencies\": {",
                    "\"dependencies\": {\n    \"@supabase/supabase-js\": \"^2.49.4\","
            );
            Files.writeString(packageJsonFile.toPath(), content, StandardCharsets.UTF_8);
            System.out.println("✓ 已添加 @supabase/supabase-js 依赖");
        } else {
            System.out.println("✓ package.json 已包含 Supabase 依赖");
        }

        // 重新读取验证
        content = Files.readString(packageJsonFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(content.contains("@supabase/supabase-js"), "应包含 @supabase/supabase-js 依赖");
        System.out.println("✓ package.json 验证通过");
    }

    /**
     * 测试 5: 验证应用信息
     */
    @Test
    @Order(5)
    void testAppInfo() {
        System.out.println("\n【测试 5】验证应用信息");

        App app = appService.getById(TEST_APP_ID);
        assertNotNull(app, "应用应存在");

        System.out.println("✓ 应用 ID: " + app.getId());
        System.out.println("✓ 代码类型: " + app.getCodeGenType());
        System.out.println("✓ 数据库状态: " + app.getHasDatabase());
    }

    @AfterAll
    static void summary() {
        System.out.println("\n========================================");
        System.out.println("集成测试完成");
        System.out.println("========================================");
    }
}
