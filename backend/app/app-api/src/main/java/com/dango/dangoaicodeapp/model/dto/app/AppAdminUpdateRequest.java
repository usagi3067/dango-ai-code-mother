package com.dango.dangoaicodeapp.model.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class AppAdminUpdateRequest implements Serializable {

    /**
     * id
     */
    @Schema(type = "string")
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 应用标签
     */
    private String tag;

    private static final long serialVersionUID = 1L;
}
