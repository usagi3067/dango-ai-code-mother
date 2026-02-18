package com.dango.dangoaicodeapp.workflow.node;

import com.dango.dangoaicodeapp.domain.app.valueobject.ElementInfo;
import com.dango.dangoaicodeapp.domain.codegen.node.CodeModifierNode;
import com.dango.dangoaicodeapp.domain.codegen.workflow.state.WorkflowContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CodeModifierNode 单元测试
 * 测试代码修改节点的核心逻辑
 *
 * @author dango
 */
class CodeModifierNodeTest {

    @Test
    @DisplayName("构建修改请求 - 包含所有信息")
    void testBuildModifyRequestWithAllInfo() {
        // 准备测试数据
        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .originalPrompt("将按钮颜色改为红色")
                .projectStructure("项目目录结构:\n- index.html\n- style.css\n")
                .elementInfo(ElementInfo.builder()
                        .tagName("BUTTON")
                        .selector("body > div > button")
                        .id("submit-btn")
                        .className("btn primary")
                        .textContent("提交")
                        .pagePath("/index.html")
                        .build())
                .build();

        // 执行测试
        String request = CodeModifierNode.buildModifyRequest(context);

        // 验证结果
        assertNotNull(request);
        assertTrue(request.contains("## 项目结构"));
        assertTrue(request.contains("index.html"));
        assertTrue(request.contains("## 选中元素信息"));
        assertTrue(request.contains("button")); // 标签名应转小写
        assertTrue(request.contains("body > div > button"));
        assertTrue(request.contains("## 修改要求"));
        assertTrue(request.contains("将按钮颜色改为红色"));
        assertTrue(request.contains("## 操作指南"));
    }

    @Test
    @DisplayName("构建修改请求 - 无项目结构")
    void testBuildModifyRequestWithoutProjectStructure() {
        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .originalPrompt("修改标题文字")
                .elementInfo(ElementInfo.builder()
                        .tagName("H1")
                        .selector("h1")
                        .textContent("原标题")
                        .build())
                .build();

        String request = CodeModifierNode.buildModifyRequest(context);

        assertNotNull(request);
        assertFalse(request.contains("## 项目结构"));
        assertTrue(request.contains("## 选中元素信息"));
        assertTrue(request.contains("## 修改要求"));
    }

    @Test
    @DisplayName("构建修改请求 - 无元素信息")
    void testBuildModifyRequestWithoutElementInfo() {
        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .originalPrompt("添加一个新的导航栏")
                .projectStructure("项目目录结构:\n- index.html\n")
                .build();

        String request = CodeModifierNode.buildModifyRequest(context);

        assertNotNull(request);
        assertTrue(request.contains("## 项目结构"));
        assertFalse(request.contains("## 选中元素信息"));
        assertTrue(request.contains("## 修改要求"));
    }

    @Test
    @DisplayName("格式化元素信息 - 完整信息")
    void testFormatElementInfoComplete() {
        ElementInfo elementInfo = ElementInfo.builder()
                .tagName("DIV")
                .selector("body > div.container")
                .id("main-container")
                .className("container flex")
                .textContent("这是容器内容")
                .pagePath("/pages/home.html")
                .build();

        String formatted = CodeModifierNode.formatElementInfo(elementInfo);

        assertNotNull(formatted);
        assertTrue(formatted.contains("- 标签: div")); // 应转小写
        assertTrue(formatted.contains("- 选择器: body > div.container"));
        assertTrue(formatted.contains("- ID: main-container"));
        assertTrue(formatted.contains("- 类名: container flex"));
        assertTrue(formatted.contains("- 当前内容: 这是容器内容"));
        assertTrue(formatted.contains("- 页面路径: /pages/home.html"));
    }

    @Test
    @DisplayName("格式化元素信息 - 部分信息")
    void testFormatElementInfoPartial() {
        ElementInfo elementInfo = ElementInfo.builder()
                .tagName("IMG")
                .selector("img.logo")
                .build();

        String formatted = CodeModifierNode.formatElementInfo(elementInfo);

        assertNotNull(formatted);
        assertTrue(formatted.contains("- 标签: img"));
        assertTrue(formatted.contains("- 选择器: img.logo"));
        assertFalse(formatted.contains("- ID:"));
        assertFalse(formatted.contains("- 类名:"));
        assertFalse(formatted.contains("- 当前内容:"));
        assertFalse(formatted.contains("- 页面路径:"));
    }

    @Test
    @DisplayName("格式化元素信息 - 空对象")
    void testFormatElementInfoNull() {
        String formatted = CodeModifierNode.formatElementInfo(null);
        assertEquals("", formatted);
    }

    @Test
    @DisplayName("格式化元素信息 - 长文本内容截断")
    void testFormatElementInfoLongTextContent() {
        String longText = "这是一段非常长的文本内容，".repeat(10); // 超过100字符
        ElementInfo elementInfo = ElementInfo.builder()
                .tagName("P")
                .selector("p")
                .textContent(longText)
                .build();

        String formatted = CodeModifierNode.formatElementInfo(elementInfo);

        assertNotNull(formatted);
        assertTrue(formatted.contains("..."));
        // 验证截断后的长度（100字符 + "..."）
        String contentLine = formatted.lines()
                .filter(line -> line.contains("- 当前内容:"))
                .findFirst()
                .orElse("");
        assertTrue(contentLine.length() < longText.length());
    }

    @Test
    @DisplayName("格式化元素信息 - 空白文本内容")
    void testFormatElementInfoBlankTextContent() {
        ElementInfo elementInfo = ElementInfo.builder()
                .tagName("SPAN")
                .selector("span")
                .textContent("   ")
                .build();

        String formatted = CodeModifierNode.formatElementInfo(elementInfo);

        assertNotNull(formatted);
        assertFalse(formatted.contains("- 当前内容:"));
    }

    @Test
    @DisplayName("构建修改请求 - 空上下文")
    void testBuildModifyRequestEmptyContext() {
        WorkflowContext context = WorkflowContext.builder().build();

        String request = CodeModifierNode.buildModifyRequest(context);

        assertNotNull(request);
        // 应该只包含操作指南
        assertTrue(request.contains("## 操作指南"));
        assertFalse(request.contains("## 项目结构"));
        assertFalse(request.contains("## 选中元素信息"));
        assertFalse(request.contains("## 修改要求"));
    }
}
