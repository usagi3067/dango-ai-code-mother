package com.dango.dangoaicodeapp.domain.codegen.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import com.dango.aicodegenerate.model.message.*;
import com.dango.dangoaicodeapp.domain.codegen.tools.BaseTool;
import com.dango.dangoaicodeapp.domain.codegen.tools.ToolManager;
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
