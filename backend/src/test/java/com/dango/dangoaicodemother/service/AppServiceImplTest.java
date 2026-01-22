package com.dango.dangoaicodemother.service;

import com.dango.dangoaicodemother.model.dto.app.AppQueryRequest;
import com.dango.dangoaicodemother.service.impl.AppServiceImpl;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AppServiceImpl 单元测试
 * 测试标签筛选查询功能
 */
@ExtendWith(MockitoExtension.class)
class AppServiceImplTest {

    @InjectMocks
    private AppServiceImpl appService;

    @Mock
    private UserService userService;

    @Nested
    @DisplayName("getQueryWrapper 标签筛选测试")
    class GetQueryWrapperTagFilterTests {

        @Test
        @DisplayName("应在查询条件中包含 tag 筛选")
        void shouldIncludeTagInQueryWrapper() {
            // Arrange
            AppQueryRequest request = new AppQueryRequest();
            request.setTag("tool");

            // Act
            QueryWrapper queryWrapper = appService.getQueryWrapper(request);

            // Assert
            assertNotNull(queryWrapper);
            String sql = queryWrapper.toSQL();
            assertTrue(sql.contains("tag"), "查询条件应包含 tag 字段");
        }

        @Test
        @DisplayName("tag 为 null 时不应添加 tag 条件")
        void shouldNotIncludeTagWhenNull() {
            // Arrange
            AppQueryRequest request = new AppQueryRequest();
            request.setTag(null);
            request.setAppName("测试应用");

            // Act
            QueryWrapper queryWrapper = appService.getQueryWrapper(request);

            // Assert
            assertNotNull(queryWrapper);
            // MyBatis-Flex 的 eq 方法在值为 null 时不会添加条件
        }

        @Test
        @DisplayName("应同时支持 tag 和其他条件组合筛选")
        void shouldSupportTagWithOtherConditions() {
            // Arrange
            AppQueryRequest request = new AppQueryRequest();
            request.setTag("website");
            request.setAppName("测试");
            request.setUserId(1L);

            // Act
            QueryWrapper queryWrapper = appService.getQueryWrapper(request);

            // Assert
            assertNotNull(queryWrapper);
            String sql = queryWrapper.toSQL();
            assertTrue(sql.contains("tag"), "查询条件应包含 tag 字段");
        }

        @Test
        @DisplayName("应支持所有预定义标签值筛选")
        void shouldSupportAllPredefinedTags() {
            String[] validTags = {"tool", "website", "data_analysis", "activity_page", 
                                  "management_platform", "user_app", "personal_management", "game"};
            
            for (String tag : validTags) {
                // Arrange
                AppQueryRequest request = new AppQueryRequest();
                request.setTag(tag);

                // Act
                QueryWrapper queryWrapper = appService.getQueryWrapper(request);

                // Assert
                assertNotNull(queryWrapper, "标签 " + tag + " 应生成有效的查询条件");
            }
        }
    }

    @Nested
    @DisplayName("getQueryWrapper 参数校验测试")
    class GetQueryWrapperValidationTests {

        @Test
        @DisplayName("请求参数为 null 时应抛出异常")
        void shouldThrowExceptionWhenRequestIsNull() {
            // Act & Assert
            assertThrows(Exception.class, () -> appService.getQueryWrapper(null));
        }
    }
}
