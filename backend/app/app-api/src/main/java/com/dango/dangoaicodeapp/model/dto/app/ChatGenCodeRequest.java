package com.dango.dangoaicodeapp.model.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对话生成代码请求 DTO
 */
@Data
public class ChatGenCodeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 应用 ID
     */
    @Schema(type = "string")
    private Long appId;

    /**
     * 用户消息
     */
    private String message;

    /**
     * 元素信息（可选，修改模式时传入）
     */
    private ElementInfoDTO elementInfo;
}
