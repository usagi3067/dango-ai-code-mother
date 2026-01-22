package com.dango.dangoaicodemother.core;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 应用名称生成集成测试
 * 测试完整的应用创建流程中名称生成功能
 * 需求: 1.1, 1.2
 */
@SpringBootTest
class AppNameGeneratorIntegrationTest {

    @Resource
    private AppNameGeneratorFacade appNameGeneratorFacade;

    @Test
    @DisplayName("集成测试：中文描述应生成有效的应用名称")
    void shouldGenerateValidAppNameForChineseDescription() {
        // Given: 中文应用描述
        String initPrompt = "一个帮助用户管理日常任务和待办事项的应用";

        // When: 调用名称生成服务
        String appName = appNameGeneratorFacade.generateAppName(initPrompt);

        // Then: 验证生成的名称符合要求
        assertNotNull(appName, "生成的名称不应为空");
        assertFalse(appName.isBlank(), "生成的名称不应为空白");
        assertTrue(appName.length() <= 20, "名称长度应不超过20个字符");
        assertFalse(appName.contains("\n"), "名称不应包含换行符");
        assertFalse(appName.contains("\r"), "名称不应包含回车符");
    }

    @Test
    @DisplayName("集成测试：英文描述应生成有效的应用名称")
    void shouldGenerateValidAppNameForEnglishDescription() {
        // Given: 英文应用描述
        String initPrompt = "A simple todo list application for managing daily tasks";

        // When: 调用名称生成服务
        String appName = appNameGeneratorFacade.generateAppName(initPrompt);

        // Then: 验证生成的名称符合要求
        assertNotNull(appName, "生成的名称不应为空");
        assertFalse(appName.isBlank(), "生成的名称不应为空白");
        assertTrue(appName.length() <= 20, "名称长度应不超过20个字符");
    }

    @Test
    @DisplayName("集成测试：短描述应生成有效的应用名称")
    void shouldGenerateValidAppNameForShortDescription() {
        // Given: 短应用描述
        String initPrompt = "计算器";

        // When: 调用名称生成服务
        String appName = appNameGeneratorFacade.generateAppName(initPrompt);

        // Then: 验证生成的名称符合要求
        assertNotNull(appName, "生成的名称不应为空");
        assertFalse(appName.isBlank(), "生成的名称不应为空白");
        assertTrue(appName.length() <= 20, "名称长度应不超过20个字符");
    }
}
