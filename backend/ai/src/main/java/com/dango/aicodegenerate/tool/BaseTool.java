package com.dango.aicodegenerate.tool;

import cn.hutool.json.JSONObject;

/**
 * 工具基类 - 定义所有工具的通用接口
 *
 * <h2>功能说明</h2>
 * 所有 AI 工具都应该继承此类，实现工具的基本信息和消息格式化方法。
 *
 * <h2>设计理念</h2>
 * <ul>
 *   <li>工具名称：对应 LangChain4j 的 @Tool 方法名</li>
 *   <li>显示名称：用于前端展示</li>
 *   <li>消息格式化：定义工具请求和执行结果的展示格式</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Component
 * public class FileWriteTool extends BaseTool {
 *
 *     @Tool("写入文件到指定路径")
 *     public String writeFile(
 *         @P("文件的相对路径") String filePath,
 *         @P("要写入文件的内容") String content
 *     ) {
 *         // 实现文件写入逻辑
 *         return "文件写入成功: " + filePath;
 *     }
 *
 *     @Override
 *     public String getToolName() {
 *         return "writeFile";
 *     }
 *
 *     @Override
 *     public String getDisplayName() {
 *         return "写入文件";
 *     }
 *
 *     @Override
 *     public String generateToolExecutedMessage(JSONObject arguments) {
 *         String filePath = arguments.getStr("filePath");
 *         String content = arguments.getStr("content");
 *         return String.format("[工具调用] 写入文件 %s\n```\n%s\n```", filePath, content);
 *     }
 * }
 * }</pre>
 *
 * <h2>扩展说明</h2>
 * 业务模块可以创建自己的工具基类，继承此类并添加业务特定的能力：
 * <pre>{@code
 * // 代码生成场景的工具基类
 * public abstract class CodeGenBaseTool extends BaseTool {
 *     protected Path getProjectRoot(Long appId) {
 *         // 代码生成特有的项目路径解析逻辑
 *     }
 * }
 *
 * // 用户管理场景的工具基类
 * public abstract class UserManagementBaseTool extends BaseTool {
 *     protected User getCurrentUser() {
 *         // 用户管理特有的用户获取逻辑
 *     }
 * }
 * }</pre>
 */
public abstract class BaseTool {

    /**
     * 获取工具的英文名称（对应 LangChain4j 的 @Tool 方法名）
     *
     * @return 工具英文名称
     */
    public abstract String getToolName();

    /**
     * 获取工具的显示名称
     *
     * @return 工具中文名称或其他语言的显示名称
     */
    public abstract String getDisplayName();

    /**
     * 生成工具请求时的显示内容
     *
     * <p>当 AI 选择使用此工具时，会调用此方法生成显示给用户的消息。
     * 子类可以覆盖此方法自定义显示格式。
     *
     * @return 工具请求显示内容
     */
    public String generateToolRequestMessage() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 生成工具执行结果的显示内容
     *
     * <p>当工具执行完成后，会调用此方法生成显示给用户的消息。
     * 子类必须实现此方法，定义如何格式化工具执行结果。
     *
     * @param arguments 工具执行的参数（JSON 对象）
     * @return 格式化的执行结果字符串
     */
    public abstract String generateToolExecutedMessage(JSONObject arguments);
}
