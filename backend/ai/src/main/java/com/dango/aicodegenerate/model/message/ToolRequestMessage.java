package com.dango.aicodegenerate.model.message;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具调用消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolRequestMessage extends StreamMessage {

    private String id;

    private String name;

    private String arguments;

    /**
     * 文件路径（流式工具专用）
     */
    private String filePath;

    /**
     * 操作类型 write/modify
     */
    private String action;

    public ToolRequestMessage(ToolExecutionRequest toolExecutionRequest) {
        super(StreamMessageTypeEnum.TOOL_REQUEST.getValue());
        this.id = toolExecutionRequest.id();
        this.name = toolExecutionRequest.name();
        this.arguments = toolExecutionRequest.arguments();
    }

    /**
     * 流式工具专用构造方法
     */
    public ToolRequestMessage(String id, String name, String filePath, String action, String arguments) {
        super(StreamMessageTypeEnum.TOOL_REQUEST.getValue());
        this.id = id;
        this.name = name;
        this.filePath = filePath;
        this.action = action;
        this.arguments = arguments;
    }
}
