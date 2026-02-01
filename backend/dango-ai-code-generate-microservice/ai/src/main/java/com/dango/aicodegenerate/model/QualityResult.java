package com.dango.aicodegenerate.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 代码质量检查结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description("代码质量检查结果")
public class QualityResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否通过质检
     */
    @Description("是否通过质检，true表示代码无严重语法错误可正常运行，false表示存在需要修复的问题")
    private Boolean isValid;

    /**
     * 错误列表
     */
    @Description("必须修复的问题列表，如语法错误、缺失文件等")
    private List<String> errors;

    /**
     * 改进建议
     */
    @Description("关于如何修复错误和改进代码的建议列表")
    private List<String> suggestions;
}
