package com.dango.dangoaicodeapp.infrastructure.ai.service;

import com.dango.dangoaicodeapp.infrastructure.ai.model.FeatureAnalysisAiResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AiFeatureAnalyzerService {
    @SystemMessage(fromResource = "prompt/feature-analyzer-system-prompt.txt")
    FeatureAnalysisAiResult analyzeFeatures(@UserMessage String userDescription);
}
