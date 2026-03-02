package com.dango.aicodegenerate.tool;

/**
 * 工具注册表接口
 *
 * <h2>功能说明</h2>
 * 定义工具管理的抽象，具体实现由业务模块提供。
 *
 * <h2>为什么需要这个接口</h2>
 * <ul>
 *   <li>ai 模块不应该知道具体有哪些工具</li>
 *   <li>不同业务模块可以有不同的工具集合</li>
 *   <li>工具的注册和管理是业务逻辑，不是技术基础设施</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Component
 * public class MyToolRegistry implements ToolRegistry {
 *
 *     private final Map<String, BaseTool> toolMap = new HashMap<>();
 *
 *     @Resource
 *     private BaseTool[] tools;  // Spring 自动注入所有 BaseTool 实现
 *
 *     @PostConstruct
 *     public void init() {
 *         for (BaseTool tool : tools) {
 *             registerTool(tool);
 *         }
 *     }
 *
 *     @Override
 *     public BaseTool getTool(String toolName) {
 *         return toolMap.get(toolName);
 *     }
 *
 *     @Override
 *     public BaseTool[] getAllTools() {
 *         return tools;
 *     }
 *
 *     @Override
 *     public void registerTool(BaseTool tool) {
 *         toolMap.put(tool.getToolName(), tool);
 *         log.info("注册工具: {} -> {}", tool.getToolName(), tool.getDisplayName());
 *     }
 * }
 * }</pre>
 */
public interface ToolRegistry {

    /**
     * 根据工具名称获取工具实例
     *
     * @param toolName 工具名称
     * @return 工具实例，如果不存在则返回 null
     */
    BaseTool getTool(String toolName);

    /**
     * 获取所有已注册的工具
     *
     * @return 工具数组
     */
    BaseTool[] getAllTools();

    /**
     * 注册一个工具
     *
     * @param tool 工具实例
     */
    void registerTool(BaseTool tool);
}
