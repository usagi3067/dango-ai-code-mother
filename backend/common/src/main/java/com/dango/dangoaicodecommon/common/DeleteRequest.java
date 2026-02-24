package com.dango.dangoaicodecommon.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    @Schema(type = "string")
    private Long id;

    private static final long serialVersionUID = 1L;
}
