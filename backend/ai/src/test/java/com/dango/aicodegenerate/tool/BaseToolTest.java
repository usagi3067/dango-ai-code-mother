package com.dango.aicodegenerate.tool;

import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseTool 单元测试
 */
class BaseToolTest {

    /**
     * 测试工具实现类
     */
    static class TestTool extends BaseTool {

        @Override
        public String getToolName() {
            return "testTool";
        }

        @Override
        public String getDisplayName() {
            return "测试工具";
        }

        @Override
        public String generateToolExecutedMessage(JSONObject arguments) {
            String param1 = arguments.getStr("param1");
            String param2 = arguments.getStr("param2");
            return String.format("[工具调用] %s - param1: %s, param2: %s",
                getDisplayName(), param1, param2);
        }
    }

    @Test
    void testGetToolName() {
        TestTool tool = new TestTool();
        assertEquals("testTool", tool.getToolName());
    }

    @Test
    void testGetDisplayName() {
        TestTool tool = new TestTool();
        assertEquals("测试工具", tool.getDisplayName());
    }

    @Test
    void testGenerateToolRequestMessage() {
        TestTool tool = new TestTool();
        String message = tool.generateToolRequestMessage();

        assertNotNull(message);
        assertTrue(message.contains("选择工具"));
        assertTrue(message.contains("测试工具"));
        assertEquals("\n\n[选择工具] 测试工具\n\n", message);
    }

    @Test
    void testGenerateToolExecutedMessage() {
        TestTool tool = new TestTool();

        JSONObject arguments = new JSONObject();
        arguments.set("param1", "value1");
        arguments.set("param2", "value2");

        String message = tool.generateToolExecutedMessage(arguments);

        assertNotNull(message);
        assertTrue(message.contains("工具调用"));
        assertTrue(message.contains("测试工具"));
        assertTrue(message.contains("value1"));
        assertTrue(message.contains("value2"));
    }

    /**
     * 测试自定义工具请求消息
     */
    static class CustomRequestMessageTool extends BaseTool {

        @Override
        public String getToolName() {
            return "customTool";
        }

        @Override
        public String getDisplayName() {
            return "自定义工具";
        }

        @Override
        public String generateToolRequestMessage() {
            return String.format(">>> 正在使用工具: %s <<<", getDisplayName());
        }

        @Override
        public String generateToolExecutedMessage(JSONObject arguments) {
            return "执行完成";
        }
    }

    @Test
    void testCustomToolRequestMessage() {
        CustomRequestMessageTool tool = new CustomRequestMessageTool();
        String message = tool.generateToolRequestMessage();

        assertEquals(">>> 正在使用工具: 自定义工具 <<<", message);
    }
}
