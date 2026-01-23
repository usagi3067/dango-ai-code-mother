package com.dango.dangoaicodemother.model.dto.chathistory;

import com.dango.dangoaicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对话历史查询请求 DTO
 *
 * @author dango
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用 ID（用于按应用过滤）
     */
    private Long appId;

    /**
     * 游标 ID（用于游标分页，向前加载更早的消息）
     */
    private Long lastId;

    /**
     * 用户 ID（管理员过滤用）
     */
    private Long userId;

    /**
     * 消息类型（user/ai）
     */
    private String messageType;
}
