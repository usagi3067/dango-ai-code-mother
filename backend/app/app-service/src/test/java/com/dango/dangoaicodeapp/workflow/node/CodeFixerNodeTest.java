package com.dango.dangoaicodeapp.workflow.node;

import com.dango.aicodegenerate.model.QualityResult;
import com.dango.dangoaicodeapp.model.enums.CodeGenTypeEnum;
import com.dango.dangoaicodeapp.workflow.state.WorkflowContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CodeFixerNode 单元测试
 * 测试代码修复节点的核心逻辑
 *
 * @author dango
 */
class CodeFixerNodeTest {

    @Test
    @DisplayName("构建修复请求 - 包含所有信息")
    void testBuildFixRequestWithAllInfo() {
        // 准备测试数据
        QualityResult qualityResult = QualityResult.builder()
                .isValid(false)
                .errors(Arrays.asList(
                        "第10行: 缺少闭合标签 </div>",
                        "第25行: CSS 属性 'colr' 拼写错误，应为 'color'"
                ))
                .suggestions(Arrays.asList(
                        "在第10行添加 </div> 闭合标签",
                        "将第25行的 'colr' 修改为 'color'"
                ))
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .originalPrompt("创建一个带有导航栏的首页")
                .qualityResult(qualityResult)
                .generationType(CodeGenTypeEnum.HTML)
                .build();

        // 执行测试
        String request = CodeFixerNode.buildFixRequest(context);

        // 验证结果
        assertNotNull(request);
        assertTrue(request.contains("## 原始需求"));
        assertTrue(request.contains("创建一个带有导航栏的首页"));
        assertTrue(request.contains("## 代码存在以下问题，请修复"));
        assertTrue(request.contains("缺少闭合标签 </div>"));
        assertTrue(request.contains("CSS 属性 'colr' 拼写错误"));
        assertTrue(request.contains("## 修复建议"));
        assertTrue(request.contains("添加 </div> 闭合标签"));
        assertTrue(request.contains("## 修复指南（HTML 单文件模式）"));
    }

    @Test
    @DisplayName("构建修复请求 - 无修复建议")
    void testBuildFixRequestWithoutSuggestions() {
        QualityResult qualityResult = QualityResult.builder()
                .isValid(false)
                .errors(List.of("JavaScript 语法错误: 未定义变量 'userName'"))
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .originalPrompt("添加用户登录功能")
                .qualityResult(qualityResult)
                .build();

        String request = CodeFixerNode.buildFixRequest(context);

        assertNotNull(request);
        assertTrue(request.contains("## 原始需求"));
        assertTrue(request.contains("## 代码存在以下问题，请修复"));
        assertTrue(request.contains("未定义变量 'userName'"));
        assertFalse(request.contains("## 修复建议"));
        assertTrue(request.contains("## 修复指南"));
    }

    @Test
    @DisplayName("构建修复请求 - 无错误列表")
    void testBuildFixRequestWithoutErrors() {
        QualityResult qualityResult = QualityResult.builder()
                .isValid(false)
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .originalPrompt("创建一个表单页面")
                .qualityResult(qualityResult)
                .build();

        String request = CodeFixerNode.buildFixRequest(context);

        assertNotNull(request);
        assertTrue(request.contains("## 原始需求"));
        assertTrue(request.contains("## 代码存在以下问题，请修复"));
        assertTrue(request.contains("代码质量检查未通过，请检查并修复潜在问题"));
        assertTrue(request.contains("## 修复指南"));
    }

    @Test
    @DisplayName("构建修复请求 - 无质检结果")
    void testBuildFixRequestWithoutQualityResult() {
        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .originalPrompt("创建一个图片画廊")
                .build();

        String request = CodeFixerNode.buildFixRequest(context);

        assertNotNull(request);
        assertTrue(request.contains("## 原始需求"));
        assertTrue(request.contains("## 代码存在以下问题，请修复"));
        assertTrue(request.contains("代码质量检查未通过，请检查并修复潜在问题"));
    }

    @Test
    @DisplayName("构建修复请求 - 无原始需求")
    void testBuildFixRequestWithoutOriginalPrompt() {
        QualityResult qualityResult = QualityResult.builder()
                .isValid(false)
                .errors(List.of("HTML 结构不完整"))
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .qualityResult(qualityResult)
                .build();

        String request = CodeFixerNode.buildFixRequest(context);

        assertNotNull(request);
        assertFalse(request.contains("## 原始需求"));
        assertTrue(request.contains("## 代码存在以下问题，请修复"));
        assertTrue(request.contains("HTML 结构不完整"));
    }

    @Test
    @DisplayName("构建修复请求 - 空错误列表")
    void testBuildFixRequestWithEmptyErrors() {
        QualityResult qualityResult = QualityResult.builder()
                .isValid(false)
                .errors(Collections.emptyList())
                .suggestions(List.of("建议添加响应式设计"))
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .originalPrompt("创建一个响应式页面")
                .qualityResult(qualityResult)
                .build();

        String request = CodeFixerNode.buildFixRequest(context);

        assertNotNull(request);
        assertTrue(request.contains("代码质量检查未通过，请检查并修复潜在问题"));
        assertTrue(request.contains("## 修复建议"));
        assertTrue(request.contains("建议添加响应式设计"));
    }

    @Test
    @DisplayName("检查是否应该继续修复 - 质检未通过且未达最大重试")
    void testShouldContinueFixWhenNotPassedAndNotMaxRetry() {
        QualityResult qualityResult = QualityResult.builder()
                .isValid(false)
                .errors(List.of("存在错误"))
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .qualityResult(qualityResult)
                .fixRetryCount(1)
                .build();

        assertTrue(CodeFixerNode.shouldContinueFix(context));
    }

    @Test
    @DisplayName("检查是否应该继续修复 - 质检未通过但达到最大重试")
    void testShouldContinueFixWhenNotPassedButMaxRetry() {
        QualityResult qualityResult = QualityResult.builder()
                .isValid(false)
                .errors(List.of("存在错误"))
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .qualityResult(qualityResult)
                .fixRetryCount(WorkflowContext.MAX_FIX_RETRY_COUNT)
                .build();

        assertFalse(CodeFixerNode.shouldContinueFix(context));
    }

    @Test
    @DisplayName("检查是否应该继续修复 - 质检通过")
    void testShouldContinueFixWhenPassed() {
        QualityResult qualityResult = QualityResult.builder()
                .isValid(true)
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .qualityResult(qualityResult)
                .fixRetryCount(1)
                .build();

        assertFalse(CodeFixerNode.shouldContinueFix(context));
    }

    @Test
    @DisplayName("检查是否应该继续修复 - 无质检结果且未达最大重试")
    void testShouldContinueFixWhenNoQualityResultAndNotMaxRetry() {
        WorkflowContext context = WorkflowContext.builder()
                .fixRetryCount(1)
                .build();

        assertTrue(CodeFixerNode.shouldContinueFix(context));
    }

    @Test
    @DisplayName("检查是否达到最大重试次数 - 未达到")
    void testHasReachedMaxRetryWhenNotReached() {
        WorkflowContext context = WorkflowContext.builder()
                .fixRetryCount(1)
                .build();

        assertFalse(CodeFixerNode.hasReachedMaxRetry(context));
    }

    @Test
    @DisplayName("检查是否达到最大重试次数 - 刚好达到")
    void testHasReachedMaxRetryWhenExactlyReached() {
        WorkflowContext context = WorkflowContext.builder()
                .fixRetryCount(WorkflowContext.MAX_FIX_RETRY_COUNT)
                .build();

        assertTrue(CodeFixerNode.hasReachedMaxRetry(context));
    }

    @Test
    @DisplayName("检查是否达到最大重试次数 - 超过")
    void testHasReachedMaxRetryWhenExceeded() {
        WorkflowContext context = WorkflowContext.builder()
                .fixRetryCount(WorkflowContext.MAX_FIX_RETRY_COUNT + 1)
                .build();

        assertTrue(CodeFixerNode.hasReachedMaxRetry(context));
    }

    @Test
    @DisplayName("检查是否达到最大重试次数 - 初始值")
    void testHasReachedMaxRetryWhenInitial() {
        WorkflowContext context = WorkflowContext.builder()
                .build();

        assertFalse(CodeFixerNode.hasReachedMaxRetry(context));
    }

    @Test
    @DisplayName("验证最大重试次数常量")
    void testMaxRetryCountConstant() {
        assertEquals(3, WorkflowContext.MAX_FIX_RETRY_COUNT);
    }

    @Test
    @DisplayName("构建修复请求 - 多个错误和建议")
    void testBuildFixRequestWithMultipleErrorsAndSuggestions() {
        QualityResult qualityResult = QualityResult.builder()
                .isValid(false)
                .errors(Arrays.asList(
                        "错误1: 缺少 DOCTYPE 声明",
                        "错误2: img 标签缺少 alt 属性",
                        "错误3: 存在未使用的 CSS 类"
                ))
                .suggestions(Arrays.asList(
                        "在文件开头添加 <!DOCTYPE html>",
                        "为所有 img 标签添加 alt 属性",
                        "删除未使用的 CSS 类或添加对应的 HTML 元素"
                ))
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .originalPrompt("创建一个产品展示页面")
                .qualityResult(qualityResult)
                .generationType(CodeGenTypeEnum.HTML)
                .build();

        String request = CodeFixerNode.buildFixRequest(context);

        // 验证错误编号
        assertTrue(request.contains("1. 错误1: 缺少 DOCTYPE 声明"));
        assertTrue(request.contains("2. 错误2: img 标签缺少 alt 属性"));
        assertTrue(request.contains("3. 错误3: 存在未使用的 CSS 类"));

        // 验证建议编号
        assertTrue(request.contains("1. 在文件开头添加 <!DOCTYPE html>"));
        assertTrue(request.contains("2. 为所有 img 标签添加 alt 属性"));
        assertTrue(request.contains("3. 删除未使用的 CSS 类或添加对应的 HTML 元素"));
    }

    // ========== 不同代码生成类型的测试 ==========

    @Test
    @DisplayName("构建修复请求 - HTML 单文件模式")
    void testBuildFixRequestForHtmlType() {
        QualityResult qualityResult = QualityResult.builder()
                .isValid(false)
                .errors(List.of("HTML 结构不完整"))
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .originalPrompt("创建一个登录页面")
                .qualityResult(qualityResult)
                .generationType(CodeGenTypeEnum.HTML)
                .build();

        String request = CodeFixerNode.buildFixRequest(context);

        assertNotNull(request);
        assertTrue(request.contains("## 修复指南（HTML 单文件模式）"));
        assertTrue(request.contains("单个 HTML 文件"));
        assertTrue(request.contains("内联 CSS 和 JS"));
        assertTrue(request.contains("最多输出 1 个 HTML 代码块"));
    }

    @Test
    @DisplayName("构建修复请求 - 多文件模式")
    void testBuildFixRequestForMultiFileType() {
        QualityResult qualityResult = QualityResult.builder()
                .isValid(false)
                .errors(List.of("CSS 文件引用错误"))
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .originalPrompt("创建一个博客页面")
                .qualityResult(qualityResult)
                .generationType(CodeGenTypeEnum.MULTI_FILE)
                .build();

        String request = CodeFixerNode.buildFixRequest(context);

        assertNotNull(request);
        assertTrue(request.contains("## 修复指南（多文件模式）"));
        assertTrue(request.contains("确定问题所在的文件（HTML/CSS/JS）"));
        assertTrue(request.contains("必须输出 3 个代码块"));
        assertTrue(request.contains("HTML + CSS + JavaScript"));
    }

    @Test
    @DisplayName("构建修复请求 - Vue 工程模式")
    void testBuildFixRequestForVueProjectType() {
        QualityResult qualityResult = QualityResult.builder()
                .isValid(false)
                .errors(List.of("Vue 组件导入错误"))
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .originalPrompt("创建一个电商网站")
                .qualityResult(qualityResult)
                .generationType(CodeGenTypeEnum.VUE_PROJECT)
                .build();

        String request = CodeFixerNode.buildFixRequest(context);

        assertNotNull(request);
        assertTrue(request.contains("## 修复指南（Vue 工程模式）"));
        assertTrue(request.contains("【目录读取工具】"));
        assertTrue(request.contains("【文件读取工具】"));
        assertTrue(request.contains("【文件修改工具】"));
        assertTrue(request.contains("【文件写入工具】"));
        assertTrue(request.contains("必须使用工具进行修复"));
    }

    @Test
    @DisplayName("获取输出格式指南 - HTML 类型")
    void testGetOutputFormatGuideForHtml() {
        String guide = CodeFixerNode.getOutputFormatGuide(CodeGenTypeEnum.HTML);

        assertNotNull(guide);
        assertTrue(guide.contains("HTML 单文件模式"));
        assertTrue(guide.contains("最多输出 1 个 HTML 代码块"));
    }

    @Test
    @DisplayName("获取输出格式指南 - Multi-File 类型")
    void testGetOutputFormatGuideForMultiFile() {
        String guide = CodeFixerNode.getOutputFormatGuide(CodeGenTypeEnum.MULTI_FILE);

        assertNotNull(guide);
        assertTrue(guide.contains("多文件模式"));
        assertTrue(guide.contains("必须输出 3 个代码块"));
    }

    @Test
    @DisplayName("获取输出格式指南 - Vue Project 类型")
    void testGetOutputFormatGuideForVueProject() {
        String guide = CodeFixerNode.getOutputFormatGuide(CodeGenTypeEnum.VUE_PROJECT);

        assertNotNull(guide);
        assertTrue(guide.contains("Vue 工程模式"));
        assertTrue(guide.contains("必须使用工具进行修复"));
    }

    @Test
    @DisplayName("获取输出格式指南 - null 类型默认为 HTML")
    void testGetOutputFormatGuideForNullType() {
        String guide = CodeFixerNode.getOutputFormatGuide(null);

        assertNotNull(guide);
        assertTrue(guide.contains("HTML 单文件模式"));
    }

    @Test
    @DisplayName("构建修复请求 - 无代码生成类型默认为 HTML")
    void testBuildFixRequestWithoutGenerationType() {
        QualityResult qualityResult = QualityResult.builder()
                .isValid(false)
                .errors(List.of("存在语法错误"))
                .build();

        WorkflowContext context = WorkflowContext.builder()
                .appId(100L)
                .originalPrompt("创建一个页面")
                .qualityResult(qualityResult)
                // 不设置 generationType
                .build();

        String request = CodeFixerNode.buildFixRequest(context);

        assertNotNull(request);
        // 默认应该使用 HTML 模式的指南
        assertTrue(request.contains("## 修复指南（HTML 单文件模式）"));
    }
}
