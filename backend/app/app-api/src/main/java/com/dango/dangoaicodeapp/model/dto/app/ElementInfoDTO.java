package com.dango.dangoaicodeapp.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 元素信息 DTO
 * 用于接收前端传递的元素信息
 *
 * @author dango
 */
@Data
public class ElementInfoDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签名（如 DIV、BUTTON）
     */
    private String tagName;

    /**
     * 元素 ID
     */
    private String id;

    /**
     * 元素类名
     */
    private String className;

    /**
     * 元素文本内容
     */
    private String textContent;

    /**
     * CSS 选择器路径
     */
    private String selector;

    /**
     * 页面路径
     */
    private String pagePath;
}
