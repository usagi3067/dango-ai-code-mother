package com.dango.dangoaicodeapp.infrastructure.ai.model;

import com.dango.dangoaicodeapp.domain.codegen.model.FeatureAnalysisConstraints;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

@Data
public class FeatureAnalysisAiResult {

    @Description("功能列表，不超过" + FeatureAnalysisConstraints.MAX_FEATURE_COUNT + "条")
    private List<FeatureItemAiResult> features;
}
