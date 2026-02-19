package com.dango.aicodegenerate.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;
import java.util.List;

@Data
public class FeatureAnalysisResult {
    @Description("功能列表，不超过6条")
    private List<FeatureItem> features;
}
