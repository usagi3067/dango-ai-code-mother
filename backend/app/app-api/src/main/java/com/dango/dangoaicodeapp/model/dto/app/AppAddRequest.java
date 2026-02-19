package com.dango.dangoaicodeapp.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    private String appName;

    private String tag;

    private String codeGenType;

    private static final long serialVersionUID = 1L;
}
