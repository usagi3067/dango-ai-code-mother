package com.dango.aicodegenerate.extractor;

import com.dango.aicodegenerate.model.message.StreamMessage;
import com.dango.aicodegenerate.model.message.ToolRequestMessage;
import com.dango.aicodegenerate.model.message.ToolStreamingMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工具参数提取器 - 状态机实现
 * <p>
 * 用于累积解析工具调用的 arguments JSON delta 片段：
 * 1. 累积 arguments delta 片段
 * 2. 状态机：INIT -> PARSING_TRIGGER_PARAM -> STREAMING_CONTENT -> DONE
 * 3. 解析 triggerParam（如 relativeFilePath）完成后发送 TOOL_REQUEST
 * 4. 解析 streamingParams（如 content）时持续发送 TOOL_STREAMING
 * 5. JSON unescape 处理（反斜杠n, 反斜杠t, 反斜杠引号, 反斜杠反斜杠, 反斜杠uXXXX）
 * 6. 处理不完整转义序列的边界情况
 */
@Slf4j
public class ToolArgumentsExtractor {

    /**
     * 工具配置
     */
    public record ToolConfig(String triggerParam, List<String> streamingParams) {}

    /**
     * 状态机状态
     */
    public enum State {
        INIT,                   // 初始状态
        PARSING_TRIGGER_PARAM,  // 正在解析触发参数
        STREAMING_CONTENT,      // 正在流式输出内容
        DONE                    // 完成
    }

    /**
     * 工具配置映射
     */
    private static final Map<String, ToolConfig> TOOL_CONFIGS = Map.of(
            "writeFile", new ToolConfig("relativeFilePath", List.of("content")),
            "modifyFile", new ToolConfig("relativeFilePath", List.of("oldContent", "newContent")),
            "readFile", new ToolConfig("relativeFilePath", List.of()),
            "readDir", new ToolConfig("relativeDirPath", List.of()),
            "deleteFile", new ToolConfig("relativeFilePath", List.of()),
            "searchContentImages", new ToolConfig("query", List.of()),
            "searchIllustrations", new ToolConfig("query", List.of()),
            "generateLogos", new ToolConfig("description", List.of()),
            "generateMermaidDiagram", new ToolConfig("mermaidCode", List.of())
    );

    /**
     * 支持流式输出的工具集合
     */
    private static final Set<String> STREAMING_TOOLS = Set.of("writeFile", "modifyFile");

    private final String toolCallId;
    private final String toolName;
    private final ToolConfig toolConfig;

    @Getter
    private State state = State.INIT;

    // 累积的原始 JSON 字符串
    private final StringBuilder rawBuffer = new StringBuilder();

    // 当前解析位置
    private int parsePosition = 0;

    // 触发参数值
    @Getter
    private String triggerParamValue;

    // 当前正在解析的流式参数名
    private String currentStreamingParam;

    // 流式参数已发送的位置
    private int streamingParamSentPosition = 0;

    // 不完整的转义序列缓冲
    private String pendingEscape = "";

    // 是否已发送 TOOL_REQUEST
    private boolean toolRequestSent = false;

    public ToolArgumentsExtractor(String toolCallId, String toolName) {
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.toolConfig = TOOL_CONFIGS.get(toolName);
    }

    /**
     * 检查工具是否支持流式输出
     */
    public static boolean isStreamingTool(String toolName) {
        return STREAMING_TOOLS.contains(toolName);
    }

    /**
     * 检查工具是否已配置
     */
    public static boolean isConfiguredTool(String toolName) {
        return TOOL_CONFIGS.containsKey(toolName);
    }

    /**
     * 处理一个 delta 片段，返回需要发送的消息列表
     */
    public List<StreamMessage> process(String delta) {
        List<StreamMessage> messages = new ArrayList<>();

        if (delta == null || delta.isEmpty() || toolConfig == null) {
            return messages;
        }

        // 累积原始数据
        rawBuffer.append(delta);
        String raw = rawBuffer.toString();

        // 根据状态处理
        switch (state) {
            case INIT -> processInit(raw, messages);
            case PARSING_TRIGGER_PARAM -> processTriggerParam(raw, messages);
            case STREAMING_CONTENT -> processStreamingContent(raw, messages);
            case DONE -> { /* 已完成，不再处理 */ }
        }

        return messages;
    }

    /**
     * 初始状态：寻找触发参数的开始
     */
    private void processInit(String raw, List<StreamMessage> messages) {
        String triggerParam = toolConfig.triggerParam();
        String searchKey = "\"" + triggerParam + "\"";

        int keyIndex = raw.indexOf(searchKey, parsePosition);
        if (keyIndex == -1) {
            return;
        }

        // 找到冒号后的引号开始位置
        int colonIndex = raw.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return;
        }

        int valueStart = raw.indexOf('"', colonIndex + 1);
        if (valueStart == -1) {
            return;
        }

        // 切换到解析触发参数状态
        state = State.PARSING_TRIGGER_PARAM;
        parsePosition = valueStart + 1;
        processTriggerParam(raw, messages);
    }

    /**
     * 解析触发参数值
     */
    private void processTriggerParam(String raw, List<StreamMessage> messages) {
        // 寻找字符串结束引号（需要处理转义）
        int endQuote = findStringEnd(raw, parsePosition);
        if (endQuote == -1) {
            return;
        }

        // 提取并 unescape 触发参数值
        String rawValue = raw.substring(parsePosition, endQuote);
        triggerParamValue = unescape(rawValue);
        parsePosition = endQuote + 1;

        // 发送 TOOL_REQUEST 消息
        if (!toolRequestSent) {
            String action = determineAction();
            ToolRequestMessage requestMessage = new ToolRequestMessage(
                    toolCallId, toolName, triggerParamValue, action, null
            );
            messages.add(requestMessage);
            toolRequestSent = true;
        }

        // 检查是否有流式参数需要处理
        if (toolConfig.streamingParams().isEmpty()) {
            state = State.DONE;
        } else {
            state = State.STREAMING_CONTENT;
            processStreamingContent(raw, messages);
        }
    }

    /**
     * 处理流式内容参数
     */
    private void processStreamingContent(String raw, List<StreamMessage> messages) {
        // 如果当前没有正在处理的流式参数，寻找下一个
        if (currentStreamingParam == null) {
            for (String param : toolConfig.streamingParams()) {
                String searchKey = "\"" + param + "\"";
                // 从 buffer 开头搜索，支持 content 在 triggerParam 前面的情况（如 Claude 模型）
                int keyIndex = raw.indexOf(searchKey);
                if (keyIndex != -1) {
                    int colonIndex = raw.indexOf(':', keyIndex + searchKey.length());
                    if (colonIndex != -1) {
                        int valueStart = raw.indexOf('"', colonIndex + 1);
                        if (valueStart != -1) {
                            currentStreamingParam = param;
                            parsePosition = valueStart + 1;
                            streamingParamSentPosition = parsePosition;
                            pendingEscape = "";
                            break;
                        }
                    }
                }
            }
        }

        if (currentStreamingParam == null) {
            return;
        }

        // 流式输出当前参数的内容
        streamCurrentParam(raw, messages);
    }

    /**
     * 流式输出当前参数内容
     */
    private void streamCurrentParam(String raw, List<StreamMessage> messages) {
        int pos = streamingParamSentPosition;
        StringBuilder unescapedDelta = new StringBuilder();

        while (pos < raw.length()) {
            char c = raw.charAt(pos);

            if (c == '"') {
                // 字符串结束
                if (!unescapedDelta.isEmpty()) {
                    messages.add(new ToolStreamingMessage(toolCallId, currentStreamingParam, unescapedDelta.toString()));
                }
                // 当前参数完成，寻找下一个流式参数
                currentStreamingParam = null;
                parsePosition = pos + 1;
                streamingParamSentPosition = pos + 1;
                pendingEscape = "";

                // 检查是否还有其他流式参数
                processStreamingContent(raw, messages);
                return;
            }

            if (c == '\\') {
                // 转义序列
                if (pos + 1 >= raw.length()) {
                    // 不完整的转义序列，等待更多数据
                    pendingEscape = "\\";
                    break;
                }

                char next = raw.charAt(pos + 1);
                if (next == 'u') {
                    // Unicode 转义序列
                    if (pos + 5 >= raw.length()) {
                        // 不完整的 Unicode 转义
                        pendingEscape = raw.substring(pos);
                        break;
                    }
                    String hex = raw.substring(pos + 2, pos + 6);
                    try {
                        int codePoint = Integer.parseInt(hex, 16);
                        unescapedDelta.append((char) codePoint);
                        pos += 6;
                    } catch (NumberFormatException e) {
                        // 无效的 Unicode 转义，原样输出
                        unescapedDelta.append(c);
                        pos++;
                    }
                } else {
                    // 其他转义序列
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
                    unescapedDelta.append(unescaped);
                    pos += 2;
                }
            } else {
                unescapedDelta.append(c);
                pos++;
            }
        }

        // 发送累积的内容
        if (!unescapedDelta.isEmpty()) {
            messages.add(new ToolStreamingMessage(toolCallId, currentStreamingParam, unescapedDelta.toString()));
        }

        streamingParamSentPosition = pos;
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
                // 跳过转义字符
                if (pos + 1 >= raw.length()) {
                    return -1; // 不完整的转义
                }
                char next = raw.charAt(pos + 1);
                if (next == 'u') {
                    // Unicode 转义需要 6 个字符
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
                    // Unicode 转义
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
