package com.dango.dangoaicodeapp.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import com.dango.aicodegenerate.model.message.*;
import com.dango.aicodegenerate.tools.BaseTool;
import com.dango.aicodegenerate.tools.ToolManager;
import com.dango.dangoaicodeapp.core.builder.VueProjectBuilder;
import com.dango.dangoaicodeapp.model.constant.AppConstant;
import com.dango.dangoaicodeapp.service.ChatHistoryService;
import com.dango.dangoaicodeuser.model.entity.User;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import com.dango.aicodegenerate.model.message.ToolStreamingMessage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JSON 消息流处理器
 * 统一处理 VUE_PROJECT 类型的流式响应
 * 支持 AI_RESPONSE、TOOL_REQUEST、TOOL_EXECUTED 三种消息类型
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ToolManager toolManager;

    /**
     * 处理 JSON 格式的流式消息
     * 注意：AppController 会将输出包装为 {d: "..."} 格式，这里不需要再包装
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        // 跟踪每个工具当前正在流式输出的参数
        Map<String, String> currentStreamingParam = new HashMap<>();
        // 缓存工具的文件路径（用于检测语言）
        Map<String, String> toolFilePaths = new HashMap<>();
        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds,
                            currentStreamingParam, toolFilePaths);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字串
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.saveAiMessage(appId, loginUser.getId(), aiResponse);

                    // 同步构建 Vue 项目
                    // 使用同步构建确保用户在 AI 回复完成时能立即预览到最新的构建结果
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                    vueProjectBuilder.buildProject(projectPath);
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.saveAiMessage(appId, loginUser.getId(), errorMessage);
                });
    }

    /**
     * 解析并收集 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder,
            Set<String> seenToolIds, Map<String, String> currentStreamingParam,
            Map<String, String> toolFilePaths) {
        // 解析 JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                // 直接拼接响应
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage msg = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = msg.getId();
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    seenToolIds.add(toolId);

                    // 流式工具：显示文件名 + 开始代码块
                    if (msg.getFilePath() != null) {
                        toolFilePaths.put(toolId, msg.getFilePath());
                        String lang = detectLanguageByPath(msg.getFilePath());
                        String toolName = msg.getName();

                        if ("writeFile".equals(toolName)) {
                            return String.format("\n📝 正在写入 `%s`\n```%s\n", msg.getFilePath(), lang);
                        } else if ("modifyFile".equals(toolName)) {
                            return String.format("\n📝 正在修改 `%s`\n\n替换前：\n```%s\n", msg.getFilePath(), lang);
                        }
                    }

                    // 非流式工具：使用原有逻辑
                    BaseTool tool = toolManager.getTool(msg.getName());
                    if (tool != null) {
                        return tool.generateToolRequestResponse();
                    }
                }
                return "";
            }
            case TOOL_STREAMING -> {
                ToolStreamingMessage msg = JSONUtil.toBean(chunk, ToolStreamingMessage.class);
                String toolId = msg.getId();
                String paramName = msg.getParamName();
                String prevParam = currentStreamingParam.get(toolId);

                StringBuilder result = new StringBuilder();

                // 检测参数切换（从 oldContent 切换到 newContent）
                if (prevParam != null && !prevParam.equals(paramName)) {
                    String filePath = toolFilePaths.get(toolId);
                    String lang = filePath != null ? detectLanguageByPath(filePath) : "";
                    result.append("\n```\n\n替换后：\n```").append(lang).append("\n");
                }

                currentStreamingParam.put(toolId, paramName);
                result.append(msg.getDelta());
                return result.toString();
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage msg = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String toolName = msg.getName();

                // 流式工具：关闭代码块
                if ("writeFile".equals(toolName) || "modifyFile".equals(toolName)) {
                    BaseTool tool = toolManager.getTool(toolName);
                    JSONObject args = JSONUtil.parseObj(msg.getArguments());
                    String result = tool.generateToolExecutedResult(args);
                    chatHistoryStringBuilder.append(result);
                    return "\n```\n✅ 完成\n";
                }

                // 非流式工具：保持原逻辑
                BaseTool tool = toolManager.getTool(toolName);
                JSONObject args = JSONUtil.parseObj(msg.getArguments());
                String result = tool.generateToolExecutedResult(args);
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }

    /**
     * 根据文件路径检测语言
     */
    private String detectLanguageByPath(String filePath) {
        if (filePath == null) return "";
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".vue")) return "vue";
        if (lower.endsWith(".js")) return "javascript";
        if (lower.endsWith(".ts")) return "typescript";
        if (lower.endsWith(".jsx")) return "jsx";
        if (lower.endsWith(".tsx")) return "tsx";
        if (lower.endsWith(".css")) return "css";
        if (lower.endsWith(".scss")) return "scss";
        if (lower.endsWith(".less")) return "less";
        if (lower.endsWith(".html")) return "html";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".md")) return "markdown";
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".py")) return "python";
        return "";
    }
}
