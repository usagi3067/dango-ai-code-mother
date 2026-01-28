package com.dango.dangoaicodeapp.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用标签
     */
    private String tag;

    private static final long serialVersionUID = 1L;
}
