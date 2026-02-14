package com.dango.aicodegenerate.model.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具内容流式输出消息
 * 用于实时传输工具参数中大段内容的增量
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolStreamingMessage extends StreamMessage {

    /**
     * 工具调用 ID，用于关联同一次工具调用
     */
    private String id;

    /**
     * 参数名称（content / oldContent / newContent）
     */
    private String paramName;

    /**
     * 本次新增的内容（已 unescape）
     */
    private String delta;

    public ToolStreamingMessage(String id, String paramName, String delta) {
        super(StreamMessageTypeEnum.TOOL_STREAMING.getValue());
        this.id = id;
        this.paramName = paramName;
        this.delta = delta;
    }
}
