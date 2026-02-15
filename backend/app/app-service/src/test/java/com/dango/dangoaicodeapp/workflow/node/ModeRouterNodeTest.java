package com.dango.dangoaicodeapp.workflow.node;

import com.dango.dangoaicodeapp.model.entity.ElementInfo;
import com.dango.dangoaicodeapp.model.enums.OperationModeEnum;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ModeRouterNode 单元测试
 * 测试模式路由节点的核心逻辑
 *
 * @author dango
 */
class ModeRouterNodeTest {

    @TempDir
    Path tempDir;

    private String originalUserDir;

    @BeforeEach
    void setUp() {
        // 保存原始 user.dir
        originalUserDir = System.getProperty("user.dir");
        // 设置临时目录作为 user.dir，以便测试代码目录检查
        System.setProperty("user.dir", tempDir.toString());
    }

    @Test
    @DisplayName("包含 elementInfo 时应路由到修改模式")
    void testRouteToModifyModeWhenElementInfoPresent() {
        // 准备：创建包含 elementInfo 的 WorkflowContext
        ElementInfo elementInfo = ElementInfo.builder()
                .tagName("DIV")
                .selector("#main-content")
                .textContent("Hello World")
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .appId(1L)
                .originalPrompt("修改这个元素的颜色")
                .elementInfo(elementInfo)
                .build();

        // 执行：判断操作模式
        OperationModeEnum mode = ModeRouterNode.determineOperationMode(context);

        // 验证：应该是修改模式
        assertEquals(OperationModeEnum.MODIFY, mode);
    }

    @Test
    @DisplayName("无历史代码时应路由到创建模式")
    void testRouteToCreateModeWhenNoExistingCode() {
        // 准备：创建不包含 elementInfo 的 WorkflowContext，且没有历史代码
        WorkflowContext context = WorkflowContext.builder()
                .appId(999L) // 使用一个不存在代码的 appId
                .originalPrompt("创建一个新的网站")
                .build();

        // 执行：判断操作模式
        OperationModeEnum mode = ModeRouterNode.determineOperationMode(context);

        // 验证：应该是创建模式
        assertEquals(OperationModeEnum.CREATE, mode);
    }

    @Test
    @DisplayName("默认应路由到创建模式")
    void testDefaultRouteToCreateMode() {
        // 准备：创建空的 WorkflowContext
        WorkflowContext context = WorkflowContext.builder()
                .appId(0L)
                .originalPrompt("测试提示词")
                .build();

        // 执行：判断操作模式
        OperationModeEnum mode = ModeRouterNode.determineOperationMode(context);

        // 验证：应该是创建模式
        assertEquals(OperationModeEnum.CREATE, mode);
    }

    @Test
    @DisplayName("hasExistingCode 应正确检测 Vue 项目代码目录")
    void testHasExistingCodeForVueProject() throws IOException {
        // 准备：创建 Vue 项目代码目录
        Long appId = 123L;
        Path codeOutputDir = tempDir.resolve("tmp/code_output");
        Files.createDirectories(codeOutputDir);
        Path vueDir = codeOutputDir.resolve("vue_project_" + appId);
        Files.createDirectories(vueDir);

        // 执行：检查是否有现有代码
        boolean hasCode = ModeRouterNode.hasExistingCode(appId);

        // 验证：应该检测到代码
        assertTrue(hasCode);
    }

    @Test
    @DisplayName("hasExistingCode 对于非 vue_project 目录应返回 false")
    void testHasExistingCodeIgnoresNonVueProjectDirs() throws IOException {
        // 准备：创建 HTML 代码目录（旧类型，不再检测）
        Long appId = 456L;
        Path codeOutputDir = tempDir.resolve("tmp/code_output");
        Files.createDirectories(codeOutputDir);
        Path htmlDir = codeOutputDir.resolve("html_" + appId);
        Files.createDirectories(htmlDir);

        // 执行：检查是否有现有代码
        boolean hasCode = ModeRouterNode.hasExistingCode(appId);

        // 验证：不应该检测到代码（只检查 vue_project_ 目录）
        assertFalse(hasCode);
    }

    @Test
    @DisplayName("hasExistingCode 对于无效 appId 应返回 false")
    void testHasExistingCodeWithInvalidAppId() {
        // 执行 & 验证：null appId
        assertFalse(ModeRouterNode.hasExistingCode(null));

        // 执行 & 验证：0 appId
        assertFalse(ModeRouterNode.hasExistingCode(0L));

        // 执行 & 验证：负数 appId
        assertFalse(ModeRouterNode.hasExistingCode(-1L));
    }

    @Test
    @DisplayName("hasLegacyHtml 应正确检测 src/legacy.html")
    void testHasLegacyHtml() throws IOException {
        Long appId = 888L;
        Path codeOutputDir = tempDir.resolve("tmp/code_output");
        Path vueDir = codeOutputDir.resolve("vue_project_" + appId);
        Path srcDir = vueDir.resolve("src");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("legacy.html"), "<html><body>Hello</body></html>");

        assertTrue(ModeRouterNode.hasLegacyHtml(appId));
    }

    @Test
    @DisplayName("hasLegacyHtml 无 legacy.html 时应返回 false")
    void testHasLegacyHtmlNotExists() throws IOException {
        Long appId = 889L;
        Path codeOutputDir = tempDir.resolve("tmp/code_output");
        Path vueDir = codeOutputDir.resolve("vue_project_" + appId);
        Files.createDirectories(vueDir.resolve("src"));

        assertFalse(ModeRouterNode.hasLegacyHtml(appId));
    }

    @Test
    @DisplayName("determineOperationMode 检测到 legacy.html 时应标记 htmlConversionRequired")
    void testDetermineOperationModeWithLegacyHtml() throws IOException {
        Long appId = 890L;
        Path codeOutputDir = tempDir.resolve("tmp/code_output");
        Path vueDir = codeOutputDir.resolve("vue_project_" + appId);
        Path srcDir = vueDir.resolve("src");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("legacy.html"), "<html><body>Test</body></html>");

        WorkflowContext context = WorkflowContext.builder()
                .appId(appId)
                .originalPrompt("修改颜色")
                .build();

        OperationModeEnum mode = ModeRouterNode.determineOperationMode(context);

        assertEquals(OperationModeEnum.MODIFY, mode);
        assertTrue(context.isHtmlConversionRequired());
    }

    @Test
    @DisplayName("routeToNextNode 应根据操作模式返回正确的路由目标")
    void testRouteToNextNode() {
        // 测试 CREATE 模式
        WorkflowContext createContext = WorkflowContext.builder()
                .operationMode(OperationModeEnum.CREATE)
                .build();
        MessagesState<String> createState = createMessagesState(createContext);
        assertEquals("create", ModeRouterNode.routeToNextNode(createState));

        // 测试 MODIFY 模式
        WorkflowContext modifyContext = WorkflowContext.builder()
                .operationMode(OperationModeEnum.MODIFY)
                .build();
        MessagesState<String> modifyState = createMessagesState(modifyContext);
        assertEquals("modify", ModeRouterNode.routeToNextNode(modifyState));

        // 测试 FIX 模式（应路由到 create）
        WorkflowContext fixContext = WorkflowContext.builder()
                .operationMode(OperationModeEnum.FIX)
                .build();
        MessagesState<String> fixState = createMessagesState(fixContext);
        assertEquals("create", ModeRouterNode.routeToNextNode(fixState));
    }

    @Test
    @DisplayName("routeToNextNode 对于空操作模式应默认路由到创建模式")
    void testRouteToNextNodeWithNullMode() {
        // 准备：创建没有设置操作模式的 context
        WorkflowContext context = WorkflowContext.builder().build();
        MessagesState<String> state = createMessagesState(context);

        // 执行 & 验证：应该默认路由到 create
        assertEquals("create", ModeRouterNode.routeToNextNode(state));
    }

    /**
     * 创建包含 WorkflowContext 的 MessagesState
     */
    private MessagesState<String> createMessagesState(WorkflowContext context) {
        Map<String, Object> data = new HashMap<>();
        data.put(WorkflowContext.WORKFLOW_CONTEXT_KEY, context);
        return new MessagesState<>(data);
    }
}
