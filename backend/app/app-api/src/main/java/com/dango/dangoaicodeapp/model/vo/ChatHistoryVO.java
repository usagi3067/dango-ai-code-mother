package com.dango.dangoaicodeapp.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史视图对象
 *
 * @author dango
 */
@Data
public class ChatHistoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Schema(type = "string")
    private Long id;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型（user/ai）
     */
    private String messageType;

    /**
     * 应用id
     */
    @Schema(type = "string")
    private Long appId;

    /**
     * 用户id
     */
    @Schema(type = "string")
    private Long userId;

    /**
     * 消息状态: generating/completed/error
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
