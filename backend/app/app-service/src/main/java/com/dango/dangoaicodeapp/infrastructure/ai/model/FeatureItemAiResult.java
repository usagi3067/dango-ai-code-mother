package com.dango.dangoaicodeapp.infrastructure.ai.model;

import com.dango.dangoaicodeapp.domain.codegen.model.FeatureAnalysisConstraints;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class FeatureItemAiResult {

    @Description("功能名称，不超过" + FeatureAnalysisConstraints.MAX_NAME_LENGTH + "个字符")
    private String name;

    @Description("功能描述，不超过" + FeatureAnalysisConstraints.MAX_DESCRIPTION_LENGTH + "个字符")
    private String description;

    @Description("是否默认勾选：用户明确提到的功能为true，AI建议的扩展功能为false")
    private boolean checked;

    @Description("是否为推荐功能：当用户描述模糊时，AI推荐的核心功能为true")
    private boolean recommended;
}
