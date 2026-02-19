package com.dango.aicodegenerate.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class FeatureItem {
    @Description("功能名称，不超过10个字符")
    private String name;

    @Description("功能描述，不超过30个字符")
    private String description;

    @Description("是否默认勾选：用户明确提到的功能为true，AI建议的扩展功能为false")
    private boolean checked;

    @Description("是否为推荐功能：当用户描述模糊时，AI推荐的核心功能为true")
    private boolean recommended;
}
