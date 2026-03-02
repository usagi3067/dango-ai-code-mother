package com.dango.aicodegenerate.tool;

/**
 * 工具配置接口
 *
 * <h2>功能说明</h2>
 * 定义工具的配置信息，用于流式工具调用的参数解析。
 *
 * <h2>为什么需要这个接口</h2>
 * 在流式工具调用中，AI 会逐步返回工具参数的 JSON 片段。
 * 为了提升用户体验，我们希望在解析到关键参数（如文件路径）时，
 * 立即向前端发送 ToolRequestMessage，而不是等待所有参数解析完成。
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Component
 * public class MyToolConfig implements ToolConfig {
 *
 *     @Override
 *     public String getTriggerParam(String toolName) {
 *         return switch (toolName) {
 *             case "writeFile" -> "filePath";
 *             case "searchUser" -> "userId";
 *             default -> null;  // 不需要提前触发
 *         };
 *     }
 *
 *     @Override
 *     public String getAction(String toolName) {
 *         return switch (toolName) {
 *             case "writeFile" -> "write";
 *             case "searchUser" -> "search";
 *             default -> "unknown";
 *         };
 *     }
 * }
 * }</pre>
 */
public interface ToolConfig {

    /**
     * 获取触发参数名
     *
     * <p>当解析到此参数时，会立即发送 ToolRequestMessage。
     *
     * @param toolName 工具名称
     * @return 触发参数名，如果不需要提前触发则返回 null
     */
    String getTriggerParam(String toolName);

    /**
     * 获取操作类型
     *
     * <p>用于 ToolRequestMessage 的 action 字段，
     * 便于前端展示不同的操作提示。
     *
     * @param toolName 工具名称
     * @return 操作类型（如 "write", "read", "search" 等）
     */
    String getAction(String toolName);
}
