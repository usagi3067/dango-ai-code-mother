package com.dango.dangoaicodeapp.model.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class AppDeployRequest implements Serializable {

    /**
     * 应用 id
     */
    @Schema(type = "string")
    private Long appId;

    private static final long serialVersionUID = 1L;
}
