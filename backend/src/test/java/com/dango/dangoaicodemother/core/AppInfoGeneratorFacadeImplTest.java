package com.dango.dangoaicodemother.core;

import com.dango.dangoaicodemother.ai.AiAppInfoGeneratorService;
import com.dango.dangoaicodemother.ai.model.AppNameAndTagResult;
import com.dango.dangoaicodemother.model.enums.AppTagEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * AppInfoGeneratorFacadeImpl 单元测试
 * 
 * 回答你的疑问：
 * 1. 会启动 Spring 容器吗？
 *    不会。这里使用的是 @ExtendWith(MockitoExtension.class) 而不是 @SpringBootTest。
 *    这是一个纯粹的 Mockito 单元测试，它只初始化相关的 Mock 对象，启动速度极快，不依赖 Spring 环境。
 * 
 * 2. 注解和方法的操作含义：
 *    - @ExtendWith(MockitoExtension.class): 启用 Mockito 扩展，让 JUnit 5 支持 Mockito 注解。
 *    - @Mock: 创建一个模拟对象（Mock Object）。这里模拟了 AiAppInfoGeneratorService，它不会执行真实逻辑。
 *    - @InjectMocks: 自动将标注了 @Mock 的对象注入到该实例中。这里将模拟的 aiAppInfoGeneratorService 注入到 facade 中。
 *    - @Nested: JUnit 5 提供的注解，用于对测试用例进行分组，使测试结构更清晰。
 *    - @Test: 标识这是一个测试方法。
 *    - @DisplayName: 为测试类或方法提供可读性更好的名称。
 *    - @BeforeEach: 在每个测试方法执行前运行，常用于初始化数据。
 *    - when(...).thenReturn(...): 打桩（Stubbing），定义 Mock 对象在接收到特定调用时的返回行为。
 *    - assertEquals/assertTrue/assertFalse: 断言，用于验证程序运行结果是否符合预期。
 */
@ExtendWith(MockitoExtension.class)
class AppInfoGeneratorFacadeImplTest {

    @Mock
    private AiAppInfoGeneratorService aiAppInfoGeneratorService;

    @InjectMocks
    private AppInfoGeneratorFacadeImpl facade;

    @Nested
    @DisplayName("generateAppInfo 正常生成流程测试")
    class GenerateAppInfoTests {

        @Test
        @DisplayName("应正确返回 AI 生成的名称和有效标签")
        void shouldReturnAiGeneratedNameAndValidTag() {
            // Arrange (准备数据): 设置 Mock 对象的行为
            AppNameAndTagResult aiResult = new AppNameAndTagResult();
            aiResult.setAppName("任务管理器");
            aiResult.setTag("tool");
            when(aiAppInfoGeneratorService.generateAppInfo(anyString())).thenReturn(aiResult);

            // Act (执行操作): 调用被测方法
            AppNameAndTagResult result = facade.generateAppInfo("帮我做一个任务管理应用");

            // Assert (验证结果): 检查输出是否正确
            assertEquals("任务管理器", result.getAppName());
            assertEquals("tool", result.getTag());
        }

        @Test
        @DisplayName("应处理空 initPrompt 并返回默认值")
        void shouldHandleEmptyInitPrompt() {
            // Act
            AppNameAndTagResult result = facade.generateAppInfo("");

            // Assert
            assertEquals("未命名应用", result.getAppName());
            assertEquals(AppTagEnum.getDefaultText(), result.getTag());
        }

        @Test
        @DisplayName("应处理 null initPrompt 并返回默认值")
        void shouldHandleNullInitPrompt() {
            // Act
            AppNameAndTagResult result = facade.generateAppInfo(null);

            // Assert
            assertEquals("未命名应用", result.getAppName());
            assertEquals(AppTagEnum.getDefaultText(), result.getTag());
        }
    }

    @Nested
    @DisplayName("无效标签降级测试")
    class InvalidTagFallbackTests {

        @Test
        @DisplayName("应将无效标签降级为默认标签")
        void shouldFallbackInvalidTagToDefault() {
            // Arrange
            AppNameAndTagResult aiResult = new AppNameAndTagResult();
            aiResult.setAppName("测试应用");
            aiResult.setTag("invalid_tag");
            when(aiAppInfoGeneratorService.generateAppInfo(anyString())).thenReturn(aiResult);

            // Act
            AppNameAndTagResult result = facade.generateAppInfo("测试描述");

            // Assert
            assertEquals("测试应用", result.getAppName());
            assertEquals(AppTagEnum.getDefaultText(), result.getTag());
        }

        @Test
        @DisplayName("应将空标签降级为默认标签")
        void shouldFallbackEmptyTagToDefault() {
            // Arrange
            AppNameAndTagResult aiResult = new AppNameAndTagResult();
            aiResult.setAppName("测试应用");
            aiResult.setTag("");
            when(aiAppInfoGeneratorService.generateAppInfo(anyString())).thenReturn(aiResult);

            // Act
            AppNameAndTagResult result = facade.generateAppInfo("测试描述");

            // Assert
            assertEquals("测试应用", result.getAppName());
            assertEquals(AppTagEnum.getDefaultText(), result.getTag());
        }

        @Test
        @DisplayName("应将 null 标签降级为默认标签")
        void shouldFallbackNullTagToDefault() {
            // Arrange
            AppNameAndTagResult aiResult = new AppNameAndTagResult();
            aiResult.setAppName("测试应用");
            aiResult.setTag(null);
            when(aiAppInfoGeneratorService.generateAppInfo(anyString())).thenReturn(aiResult);

            // Act
            AppNameAndTagResult result = facade.generateAppInfo("测试描述");

            // Assert
            assertEquals("测试应用", result.getAppName());
            assertEquals(AppTagEnum.getDefaultText(), result.getTag());
        }
    }

    @Nested
    @DisplayName("AI 调用失败降级测试")
    class AiFailureFallbackTests {

        @Test
        @DisplayName("AI 调用异常时应使用降级值")
        void shouldUseFallbackWhenAiThrowsException() {
            // Arrange: 模拟 AI 服务抛出异常
            when(aiAppInfoGeneratorService.generateAppInfo(anyString()))
                    .thenThrow(new RuntimeException("AI service unavailable"));

            // Act
            AppNameAndTagResult result = facade.generateAppInfo("这是一个很长的应用描述文本");

            // Assert
            assertEquals("这是一个很长的应用描述文", result.getAppName());
            assertEquals(AppTagEnum.getDefaultText(), result.getTag());
        }

        @Test
        @DisplayName("AI 返回空名称时应使用降级名称")
        void shouldUseFallbackNameWhenAiReturnsEmptyName() {
            // Arrange
            AppNameAndTagResult aiResult = new AppNameAndTagResult();
            aiResult.setAppName("");
            aiResult.setTag("tool");
            when(aiAppInfoGeneratorService.generateAppInfo(anyString())).thenReturn(aiResult);

            // Act
            AppNameAndTagResult result = facade.generateAppInfo("短描述");

            // Assert
            assertEquals("短描述", result.getAppName());
            assertEquals("tool", result.getTag());
        }
    }

    @Nested
    @DisplayName("sanitizeName 名称清理逻辑测试")
    class SanitizeNameTests {

        private AppInfoGeneratorFacadeImpl facadeInstance;

        @BeforeEach
        void setUp() {
            facadeInstance = new AppInfoGeneratorFacadeImpl();
        }

        @Test
        @DisplayName("应去除首尾空白字符")
        void shouldTrimWhitespace() {
            String result = facadeInstance.sanitizeName("  应用名称  ");
            assertEquals("应用名称", result);
        }

        @Test
        @DisplayName("应去除换行符")
        void shouldRemoveNewlines() {
            String result = facadeInstance.sanitizeName("应用\n名称\r测试");
            assertEquals("应用名称测试", result);
        }

        @Test
        @DisplayName("应去除特殊字符")
        void shouldRemoveSpecialChars() {
            String result = facadeInstance.sanitizeName("应用@名称#测试$");
            assertEquals("应用名称测试", result);
        }

        @Test
        @DisplayName("应截断超过20字符的名称")
        void shouldTruncateLongNames() {
            String longName = "这是一个非常非常非常非常非常长的应用名称";
            String result = facadeInstance.sanitizeName(longName);
            assertEquals(20, result.length());
        }

        @Test
        @DisplayName("应处理 null 输入")
        void shouldHandleNullInput() {
            String result = facadeInstance.sanitizeName(null);
            assertEquals("", result);
        }
    }

    @Nested
    @DisplayName("fallbackName 降级逻辑测试")
    class FallbackNameTests {

        private AppInfoGeneratorFacadeImpl facadeInstance;

        @BeforeEach
        void setUp() {
            facadeInstance = new AppInfoGeneratorFacadeImpl();
        }

        @Test
        @DisplayName("应返回 initPrompt 前12位")
        void shouldReturnFirst12Chars() {
            String result = facadeInstance.fallbackName("这是一个很长的应用描述文本");
            assertEquals(12, result.length());
        }

        @Test
        @DisplayName("短于12位时应返回完整字符串")
        void shouldReturnFullStringIfShorterThan12() {
            String result = facadeInstance.fallbackName("短描述");
            assertEquals("短描述", result);
        }

        @Test
        @DisplayName("应处理 null 输入")
        void shouldHandleNullInput() {
            String result = facadeInstance.fallbackName(null);
            assertEquals("未命名应用", result);
        }

        @Test
        @DisplayName("应处理空白字符串")
        void shouldHandleBlankString() {
            String result = facadeInstance.fallbackName("   ");
            assertEquals("未命名应用", result);
        }
    }

    @Nested
    @DisplayName("isValidTag 标签验证测试")
    class IsValidTagTests {

        private AppInfoGeneratorFacadeImpl facadeInstance;

        @BeforeEach
        void setUp() {
            facadeInstance = new AppInfoGeneratorFacadeImpl();
        }

        @Test
        @DisplayName("应验证所有有效标签")
        void shouldValidateAllValidTags() {
            assertTrue(facadeInstance.isValidTag("tool"));
            assertTrue(facadeInstance.isValidTag("website"));
            assertTrue(facadeInstance.isValidTag("data_analysis"));
            assertTrue(facadeInstance.isValidTag("activity_page"));
            assertTrue(facadeInstance.isValidTag("management_platform"));
            assertTrue(facadeInstance.isValidTag("user_app"));
            assertTrue(facadeInstance.isValidTag("personal_management"));
            assertTrue(facadeInstance.isValidTag("game"));
        }

        @Test
        @DisplayName("应拒绝无效标签")
        void shouldRejectInvalidTags() {
            assertFalse(facadeInstance.isValidTag("invalid"));
            assertFalse(facadeInstance.isValidTag(""));
            assertFalse(facadeInstance.isValidTag(null));
        }
    }
}
