package com.dango.dangoaicodemother.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AppNameGeneratorFacadeImpl 单元测试
 * 测试名称清理逻辑和降级逻辑
 */
class AppNameGeneratorFacadeImplTest {

    private AppNameGeneratorFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new AppNameGeneratorFacadeImpl();
    }

    @Nested
    @DisplayName("sanitize 名称清理逻辑测试")
    class SanitizeTests {

        @Test
        @DisplayName("应去除首尾空白字符")
        void shouldTrimWhitespace() {
            String result = facade.sanitize("  应用名称  ");
            assertEquals("应用名称", result);
        }

        @Test
        @DisplayName("应去除换行符")
        void shouldRemoveNewlines() {
            String result = facade.sanitize("应用\n名称\r测试");
            assertEquals("应用名称测试", result);
        }

        @Test
        @DisplayName("应去除特殊字符")
        void shouldRemoveSpecialChars() {
            String result = facade.sanitize("应用@名称#测试$");
            assertEquals("应用名称测试", result);
        }

        @Test
        @DisplayName("应截断超过20字符的名称")
        void shouldTruncateLongNames() {
            String longName = "这是一个非常非常非常非常非常长的应用名称";
            String result = facade.sanitize(longName);
            assertEquals(20, result.length());
            assertEquals("这是一个非常非常非常非常非常长的应用名称".substring(0, 20), result);
        }

        @Test
        @DisplayName("应保留正常名称不变")
        void shouldKeepNormalNameUnchanged() {
            String result = facade.sanitize("任务管理器");
            assertEquals("任务管理器", result);
        }

        @Test
        @DisplayName("应处理null输入")
        void shouldHandleNullInput() {
            String result = facade.sanitize(null);
            assertEquals("", result);
        }

        @Test
        @DisplayName("应处理空字符串")
        void shouldHandleEmptyString() {
            String result = facade.sanitize("");
            assertEquals("", result);
        }
    }

    @Nested
    @DisplayName("fallbackName 降级逻辑测试")
    class FallbackNameTests {

        @Test
        @DisplayName("应返回initPrompt前12位")
        void shouldReturnFirst12Chars() {
            String result = facade.fallbackName("这是一个很长的应用描述文本");
            assertEquals(12, result.length());
            assertEquals("这是一个很长的应用描述文本".substring(0, 12), result);
        }

        @Test
        @DisplayName("短于12位时应返回完整字符串")
        void shouldReturnFullStringIfShorterThan12() {
            String result = facade.fallbackName("短描述");
            assertEquals("短描述", result);
        }

        @Test
        @DisplayName("应处理null输入")
        void shouldHandleNullInput() {
            String result = facade.fallbackName(null);
            assertEquals("未命名应用", result);
        }

        @Test
        @DisplayName("应处理空字符串")
        void shouldHandleEmptyString() {
            String result = facade.fallbackName("");
            assertEquals("未命名应用", result);
        }

        @Test
        @DisplayName("应处理空白字符串")
        void shouldHandleBlankString() {
            String result = facade.fallbackName("   ");
            assertEquals("未命名应用", result);
        }
    }
}
