package com.dango.dangoaicodeapp.domain.app.entity;

import com.dango.dangoaicodeapp.domain.app.valueobject.CodeGenTypeEnum;
import com.dango.dangoaicodecommon.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    @Nested
    @DisplayName("checkOwnership - 权限校验")
    class CheckOwnership {
        @Test
        void 所有者校验通过() {
            App app = App.builder().userId(100L).build();
            assertDoesNotThrow(() -> app.checkOwnership(100L));
        }

        @Test
        void 非所有者抛出异常() {
            App app = App.builder().userId(100L).build();
            assertThrows(BusinessException.class, () -> app.checkOwnership(999L));
        }
    }

    @Nested
    @DisplayName("markDeployed - 标记部署")
    class MarkDeployed {
        @Test
        void 设置deployKey和部署时间() {
            App app = new App();
            app.markDeployed("abc123");
            assertEquals("abc123", app.getDeployKey());
            assertNotNull(app.getDeployedTime());
        }
    }

    @Nested
    @DisplayName("enableDatabase - 启用数据库")
    class EnableDatabase {
        @Test
        void 正常启用() {
            App app = App.builder()
                    .hasDatabase(false)
                    .codeGenType(CodeGenTypeEnum.VUE_PROJECT.getValue())
                    .build();
            app.enableDatabase();
            assertTrue(app.getHasDatabase());
            assertNotNull(app.getEditTime());
        }

        @Test
        void 已启用时抛异常() {
            App app = App.builder()
                    .hasDatabase(true)
                    .codeGenType(CodeGenTypeEnum.VUE_PROJECT.getValue())
                    .build();
            assertThrows(BusinessException.class, () -> app.enableDatabase());
        }

        @Test
        void 非VUE_PROJECT类型抛异常() {
            App app = App.builder()
                    .hasDatabase(false)
                    .codeGenType("other_type")
                    .build();
            assertThrows(BusinessException.class, () -> app.enableDatabase());
        }
    }

    @Nested
    @DisplayName("updateInfo - 更新信息")
    class UpdateInfo {
        @Test
        void 更新名称和标签() {
            App app = new App();
            app.updateInfo("新名称", "新标签");
            assertEquals("新名称", app.getAppName());
            assertEquals("新标签", app.getTag());
            assertNotNull(app.getEditTime());
        }

        @Test
        void null值不覆盖() {
            App app = App.builder().appName("原名称").tag("原标签").build();
            app.updateInfo(null, null);
            assertEquals("原名称", app.getAppName());
            assertEquals("原标签", app.getTag());
            assertNull(app.getEditTime());
        }
    }

    @Nested
    @DisplayName("updateCover - 更新封面")
    class UpdateCover {
        @Test
        void 设置封面URL() {
            App app = new App();
            app.updateCover("https://example.com/cover.png");
            assertEquals("https://example.com/cover.png", app.getCover());
        }
    }

    @Nested
    @DisplayName("getProjectDirName - 项目目录名")
    class GetProjectDirName {
        @Test
        void 返回正确格式() {
            App app = App.builder()
                    .id(123L)
                    .codeGenType("vue_project")
                    .build();
            assertEquals("vue_project_123", app.getProjectDirName());
        }
    }

    @Nested
    @DisplayName("createNew - 工厂方法")
    class CreateNew {
        @Test
        void 创建新应用() {
            App app = App.createNew(100L, "做一个博客", "我的博客", "博客");
            assertEquals(100L, app.getUserId());
            assertEquals("做一个博客", app.getInitPrompt());
            assertEquals("我的博客", app.getAppName());
            assertEquals("博客", app.getTag());
            assertEquals(CodeGenTypeEnum.VUE_PROJECT.getValue(), app.getCodeGenType());
        }

        @Test
        void initPrompt为空时抛异常() {
            assertThrows(BusinessException.class,
                    () -> App.createNew(100L, "", "名称", "标签"));
        }
    }
}
