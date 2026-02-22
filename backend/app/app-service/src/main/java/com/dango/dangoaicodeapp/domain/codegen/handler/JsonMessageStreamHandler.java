package com.dango.dangoaicodeapp.domain.codegen.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import com.dango.aicodegenerate.model.message.*;
import com.dango.dangoaicodeapp.domain.codegen.tools.BaseTool;
import com.dango.dangoaicodeapp.domain.codegen.tools.ToolManager;
import com.dango.dangoaicodeapp.application.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

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
    private ToolManager toolManager;

    /**
     * 处理 JSON 格式的流式消息
     * 注意：AppController 会将输出包装为 {d: "..."} 格式，这里不需要再包装
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param userId             用户ID
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, long userId) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字串
                .doOnComplete(() -> {
                    // 流式响应完成后，保存 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.saveAiMessage(appId, userId, aiResponse);
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.saveAiMessage(appId, userId, errorMessage);
                });
    }

    /**
     * 处理 JSON 消息流但不保存到 chatHistory
     * 用于后台生成任务（由调用方负责保存）
     */
    public Flux<String> handleWithoutSave(Flux<String> originFlux) {
        StringBuilder dummyBuilder = new StringBuilder();
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> handleJsonMessageChunk(chunk, dummyBuilder, seenToolIds))
                .filter(StrUtil::isNotEmpty);
    }

    /**
     * 解析并收集 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder,
            Set<String> seenToolIds) {
        // 解析 JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                String msgType = aiMessage.getMsgType();
                // 只有非 log 类型的内容才记录到对话历史
                if (!"log".equals(msgType)) {
                    chatHistoryStringBuilder.append(data);
                }
                // 返回带 msgType 的 JSON，供下游区分消息类型
                if (msgType != null) {
                    return JSONUtil.toJsonStr(Map.of("d", data, "msgType", msgType));
                }
                return JSONUtil.toJsonStr(Map.of("d", data));
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage msg = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = msg.getId();
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    seenToolIds.add(toolId);

                    // 文件操作工具：显示文件名
                    if (msg.getFilePath() != null) {
                        String toolName = msg.getName();

                        if ("writeFile".equals(toolName)) {
                            return JSONUtil.toJsonStr(Map.of("d", String.format("\n📝 正在写入 `%s`...\n", msg.getFilePath())));
                        } else if ("modifyFile".equals(toolName)) {
                            return JSONUtil.toJsonStr(Map.of("d", String.format("\n📝 正在修改 `%s`...\n", msg.getFilePath())));
                        }
                    }

                    // 非流式工具：使用原有逻辑
                    BaseTool tool = toolManager.getTool(msg.getName());
                    if (tool != null) {
                        return JSONUtil.toJsonStr(Map.of("d", tool.generateToolRequestResponse()));
                    }
                }
                return "";
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage msg = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String toolName = msg.getName();

                // 所有工具统一：工具执行完成后展示完整结果
                BaseTool tool = toolManager.getTool(toolName);
                JSONObject args = JSONUtil.parseObj(msg.getArguments());
                String result = tool.generateToolExecutedResult(args);
                chatHistoryStringBuilder.append(result);
                return JSONUtil.toJsonStr(Map.of("d", String.format("\n%s\n", result)));
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }

}
