package com.dango.dangoaicodeapp.domain.codegen.ai.service;

import com.dango.aicodegenerate.model.FeatureAnalysisResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AiFeatureAnalyzerService {
    @SystemMessage(fromResource = "prompt/feature-analyzer-system-prompt.txt")
    FeatureAnalysisResult analyzeFeatures(@UserMessage String userDescription);
}
