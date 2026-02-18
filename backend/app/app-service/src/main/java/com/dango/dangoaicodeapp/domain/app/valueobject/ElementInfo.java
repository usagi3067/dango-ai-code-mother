package com.dango.dangoaicodeapp.domain.app.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 元素信息
 * 存储前端可视化编辑器选中的 DOM 元素详细信息
 *
 * @author dango
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElementInfo implements Serializable {

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
