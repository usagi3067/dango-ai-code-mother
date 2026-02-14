package com.dango.dangoaicodeapp.workflow.node;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CodeQualityCheckNode 单元测试
 * 测试转义逻辑
 */
class CodeQualityCheckNodeTest {

    /**
     * 通过反射调用 private 方法 escapeTemplateVariables
     */
    private String escapeTemplateVariables(String content) throws Exception {
        Method method = CodeQualityCheckNode.class.getDeclaredMethod("escapeTemplateVariables", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, content);
    }

    @Test
    @DisplayName("测试 Vue 模板语法 - 不在引号内应该被转义")
    void testVueTemplateOutsideQuotes() throws Exception {
        String input = "<div>{{ article.title }}</div>";
        String output = escapeTemplateVariables(input);

        System.out.println("输入: " + input);
        System.out.println("输出: " + output);

        // {{ 在不在引号内，应该被转义
        assertTrue(output.contains("{ {"), "应该被转义为 { {");
        assertTrue(output.contains("} }"), "应该被转义为 } }");
    }

    @Test
    @DisplayName("测试 Vue 模板语法 - 在双引号内应该保持不变")
    void testVueTemplateInsideDoubleQuotes() throws Exception {
        String input = "<input value=\"{{ message }}\">";
        String output = escapeTemplateVariables(input);

        System.out.println("输入: " + input);
        System.out.println("输出: " + output);

        // {{ 在双引号内，应该保持不变
        assertTrue(output.contains("\"{{"), "在双引号内应该保持 {{");
        assertTrue(output.contains("}}\""), "在双引号内应该保持 }}");
    }

    @Test
    @DisplayName("测试 Vue 模板语法 - 在单引号内应该保持不变")
    void testVueTemplateInsideSingleQuotes() throws Exception {
        String input = "<input value='{{ message }}'>";
        String output = escapeTemplateVariables(input);

        System.out.println("输入: " + input);
        System.out.println("输出: " + output);

        // {{ 在单引号内，应该保持不变
        assertTrue(output.contains("'{{"), "在单引号内应该保持 {{");
        assertTrue(output.contains("}}'"), "在单引号内应该保持 }}");
    }

    @Test
    @DisplayName("测试混合场景")
    void testMixedScenario() throws Exception {
        String input = """
            <div>{{ article.title }}</div>
            <input value="{{ message }}">
            <span>{{ count }}</span>
            """;

        String output = escapeTemplateVariables(input);

        System.out.println("========== 混合场景测试 ==========");
        System.out.println("输入:");
        System.out.println(input);
        System.out.println("\n输出:");
        System.out.println(output);

        // 不在引号内的 {{ 应该被转义
        assertTrue(output.contains("{ { article.title } }"));
        assertTrue(output.contains("{ { count } }"));

        // 在引号内的 {{ 应该保持不变
        assertTrue(output.contains("\"{{ message }}\""));
    }

    @Test
    @DisplayName("测试 LangChain4j 原始场景 - 完整 Vue 文件")
    void testFullVueFile() throws Exception {
        String vueCode = """
            <template>
              <div class="article">
                <h1>{{ article.title }}</h1>
                <p>{{ article.excerpt }}</p>
                <span>{{ article.author.name }}</span>
                <input v-model="{{ binding }}">
                <button :class="{{ btnClass }}">Click</button>
              </div>
            </template>
            """;

        String output = escapeTemplateVariables(vueCode);

        System.out.println("========== 完整 Vue 文件测试 ==========");
        System.out.println("输入:");
        System.out.println(vueCode);
        System.out.println("\n输出:");
        System.out.println(output);
    }

    @Test
    @DisplayName("测试空输入")
    void testEmptyInput() throws Exception {
        assertNull(escapeTemplateVariables(null));
        assertEquals("", escapeTemplateVariables(""));
    }
}
