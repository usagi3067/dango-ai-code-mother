package com.dango.aicodegenerate.extractor;

import com.dango.aicodegenerate.model.message.StreamMessage;
import com.dango.aicodegenerate.model.message.ToolRequestMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工具参数提取器 - 状态机实现
 * <p>
 * 用于累积解析工具调用的 arguments JSON delta 片段：
 * 1. 累积 arguments delta 片段
 * 2. 状态机：INIT -> PARSING_TRIGGER_PARAM -> DONE
 * 3. 解析 triggerParam（如 relativeFilePath）完成后发送 TOOL_REQUEST
 * 4. JSON unescape 处理（反斜杠n, 反斜杠t, 反斜杠引号, 反斜杠反斜杠, 反斜杠uXXXX）
 */
@Slf4j
public class ToolArgumentsExtractor {

    /**
     * 工具配置：triggerParam 为触发参数名，解析完成后立即发送 TOOL_REQUEST
     */
    private static final Map<String, String> TOOL_TRIGGER_PARAMS = Map.of(
            "writeFile", "relativeFilePath",
            "modifyFile", "relativeFilePath",
            "readFile", "relativeFilePath",
            "readDir", "relativeDirPath",
            "deleteFile", "relativeFilePath",
            "searchContentImages", "query",
            "searchIllustrations", "query",
            "generateLogos", "description",
            "generateMermaidDiagram", "mermaidCode"
    );

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

    public ToolArgumentsExtractor(String toolCallId, String toolName) {
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.triggerParam = TOOL_TRIGGER_PARAMS.get(toolName);
    }

    /**
     * 检查工具是否已配置
     */
    public static boolean isConfiguredTool(String toolName) {
        return TOOL_TRIGGER_PARAMS.containsKey(toolName);
    }

    /**
     * 处理一个 delta 片段，返回需要发送的消息列表
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
            String action = determineAction();
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
     * 确定操作类型
     */
    private String determineAction() {
        return switch (toolName) {
            case "writeFile" -> "write";
            case "modifyFile" -> "modify";
            case "readFile", "readDir" -> "read";
            case "deleteFile" -> "delete";
            case "searchContentImages", "searchIllustrations" -> "search";
            case "generateLogos", "generateMermaidDiagram" -> "generate";
            default -> "unknown";
        };
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
