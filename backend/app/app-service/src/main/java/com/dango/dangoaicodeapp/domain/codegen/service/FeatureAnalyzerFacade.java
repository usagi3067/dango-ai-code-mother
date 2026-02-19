package com.dango.dangoaicodeapp.domain.codegen.service;

import com.dango.aicodegenerate.model.FeatureAnalysisResult;

public interface FeatureAnalyzerFacade {
    FeatureAnalysisResult analyzeFeatures(String prompt, String supplement);
}
