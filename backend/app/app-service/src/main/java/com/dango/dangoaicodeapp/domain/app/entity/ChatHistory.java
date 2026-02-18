package com.dango.dangoaicodeapp.domain.app.entity;

import com.dango.dangoaicodeapp.model.enums.MessageTypeEnum;
import com.dango.dangoaicodecommon.exception.BusinessException;
import com.dango.dangoaicodecommon.exception.ErrorCode;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史 实体类。
 *
 * @author dango
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_history")
public class ChatHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 消息
     */
    private String message;

    /**
     * user/ai
     */
    @Column("messageType")
    private String messageType;

    /**
     * 应用id
     */
    @Column("appId")
    private Long appId;

    /**
     * 创建用户id
     */
    @Column("userId")
    private Long userId;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

    // ========== 业务方法（充血模型核心）==========

    /**
     * 创建用户消息
     */
    public static ChatHistory createUserMessage(Long appId, Long userId, String message) {
        validateMessage(appId, userId, message);
        return ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .message(message)
                .messageType(MessageTypeEnum.USER.getValue())
                .build();
    }

    /**
     * 创建 AI 消息
     */
    public static ChatHistory createAiMessage(Long appId, Long userId, String message) {
        validateMessage(appId, userId, message);
        return ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .message(message)
                .messageType(MessageTypeEnum.AI.getValue())
                .build();
    }

    /**
     * 是否为用户消息
     */
    public boolean isUserMessage() {
        return MessageTypeEnum.USER.getValue().equals(this.messageType);
    }

    /**
     * 是否为 AI 消息
     */
    public boolean isAiMessage() {
        return MessageTypeEnum.AI.getValue().equals(this.messageType);
    }

    /**
     * 校验消息参数
     */
    public static void validateMessage(Long appId, Long userId, String message) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }
        if (message == null || message.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }
    }

}
