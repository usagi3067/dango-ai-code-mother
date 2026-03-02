package com.dango.aicodegenerate.extractor;

import com.dango.aicodegenerate.model.message.StreamMessage;
import com.dango.aicodegenerate.model.message.ToolRequestMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具参数提取器 - 状态机实现
 *
 * <h2>功能说明</h2>
 * 用于累积解析工具调用的 arguments JSON delta 片段。
 * 使用状态机管理解析过程，当解析到触发参数时立即发送消息。
 *
 * <h2>工作原理</h2>
 * <ol>
 *   <li>累积 arguments delta 片段</li>
 *   <li>状态机：INIT -> PARSING_TRIGGER_PARAM -> DONE</li>
 *   <li>解析 triggerParam（如 relativeFilePath）完成后发送 TOOL_REQUEST</li>
 *   <li>处理 JSON 转义字符（\n, \t, \", \\, \uXXXX）</li>
 * </ol>
 *
 * <h2>使用说明</h2>
 * 此类通常不需要直接使用，由 StreamingResponseProcessor 自动创建和管理。
 *
 * <h2>示例</h2>
 * <pre>{@code
 * // 创建提取器
 * ToolArgumentsExtractor extractor = new ToolArgumentsExtractor(
 *     "tool-123",           // 工具调用 ID
 *     "writeFile",          // 工具名称
 *     "relativeFilePath",   // 触发参数名
 *     "write"               // 操作类型
 * );
 *
 * // 处理 delta 片段
 * String delta1 = "{\"relativeFilePath\":\"src/";
 * List<StreamMessage> messages1 = extractor.process(delta1);  // 返回空列表
 *
 * String delta2 = "App.vue\",\"content\":\"...";
 * List<StreamMessage> messages2 = extractor.process(delta2);  // 返回 ToolRequestMessage
 * }</pre>
 */
@Slf4j
public class ToolArgumentsExtractor {

    /**
     * 状态机状态
     */
    public enum State {
        INIT,                   // 初始状态
        PARSING_TRIGGER_PARAM,  // 正在解析触发参数
        DONE                    // 完成
    }

    private final String toolCallId;
    private final String toolName;
    private final String triggerParam;
    private final String action;

    @Getter
    private State state = State.INIT;

    // 累积的原始 JSON 字符串
    private final StringBuilder rawBuffer = new StringBuilder();

    // 当前解析位置
    private int parsePosition = 0;

    // 触发参数值
    @Getter
    private String triggerParamValue;

    // 是否已发送 TOOL_REQUEST
    private boolean toolRequestSent = false;

    /**
     * 构造函数
     *
     * @param toolCallId 工具调用 ID
     * @param toolName 工具名称
     * @param triggerParam 触发参数名，解析到此参数时立即发送消息
     * @param action 操作类型（write, read, search 等）
     */
    public ToolArgumentsExtractor(
        String toolCallId,
        String toolName,
        String triggerParam,
        String action
    ) {
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.triggerParam = triggerParam;
        this.action = action;
    }

    /**
     * 处理一个 delta 片段，返回需要发送的消息列表
     *
     * @param delta JSON delta 片段
     * @return 需要发送的消息列表（可能为空）
     */
    public List<StreamMessage> process(String delta) {
        List<StreamMessage> messages = new ArrayList<>();

        if (delta == null || delta.isEmpty() || triggerParam == null) {
            return messages;
        }

        // 累积原始数据
        rawBuffer.append(delta);
        String raw = rawBuffer.toString();

        // 根据状态处理
        switch (state) {
            case INIT -> processInit(raw, messages);
            case PARSING_TRIGGER_PARAM -> processTriggerParam(raw, messages);
            case DONE -> { /* 已完成，不再处理 */ }
        }

        return messages;
    }

    /**
     * 初始状态：寻找触发参数的开始
     */
    private void processInit(String raw, List<StreamMessage> messages) {
        String searchKey = "\"" + triggerParam + "\"";

        int keyIndex = raw.indexOf(searchKey, parsePosition);
        if (keyIndex == -1) {
            return;
        }

        int colonIndex = raw.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return;
        }

        int valueStart = raw.indexOf('"', colonIndex + 1);
        if (valueStart == -1) {
            return;
        }

        state = State.PARSING_TRIGGER_PARAM;
        parsePosition = valueStart + 1;
        processTriggerParam(raw, messages);
    }

    /**
     * 解析触发参数值
     */
    private void processTriggerParam(String raw, List<StreamMessage> messages) {
        int endQuote = findStringEnd(raw, parsePosition);
        if (endQuote == -1) {
            return;
        }

        String rawValue = raw.substring(parsePosition, endQuote);
        triggerParamValue = unescape(rawValue);

        if (!toolRequestSent) {
            messages.add(new ToolRequestMessage(toolCallId, toolName, triggerParamValue, action, null));
            toolRequestSent = true;
        }

        state = State.DONE;
    }

    /**
     * 查找 JSON 字符串的结束引号位置（处理转义）
     */
    private int findStringEnd(String raw, int start) {
        int pos = start;
        while (pos < raw.length()) {
            char c = raw.charAt(pos);
            if (c == '"') {
                return pos;
            }
            if (c == '\\') {
                if (pos + 1 >= raw.length()) {
                    return -1;
                }
                char next = raw.charAt(pos + 1);
                if (next == 'u') {
                    if (pos + 5 >= raw.length()) {
                        return -1;
                    }
                    pos += 6;
                } else {
                    pos += 2;
                }
            } else {
                pos++;
            }
        }
        return -1;
    }

    /**
     * JSON 字符串 unescape
     */
    private String unescape(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }

        StringBuilder result = new StringBuilder();
        int pos = 0;

        while (pos < raw.length()) {
            char c = raw.charAt(pos);
            if (c == '\\' && pos + 1 < raw.length()) {
                char next = raw.charAt(pos + 1);
                if (next == 'u' && pos + 5 < raw.length()) {
                    String hex = raw.substring(pos + 2, pos + 6);
                    try {
                        int codePoint = Integer.parseInt(hex, 16);
                        result.append((char) codePoint);
                        pos += 6;
                        continue;
                    } catch (NumberFormatException e) {
                        // 无效的 Unicode，原样输出
                    }
                }
                char unescaped = switch (next) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'r' -> '\r';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    case '/' -> '/';
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    default -> next;
                };
                result.append(unescaped);
                pos += 2;
            } else {
                result.append(c);
                pos++;
            }
        }

        return result.toString();
    }

    /**
     * 获取完整的累积 JSON
     */
    public String getRawArguments() {
        return rawBuffer.toString();
    }

    /**
     * 是否已完成解析
     */
    public boolean isDone() {
        return state == State.DONE;
    }
}
