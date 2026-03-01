package com.dango.dangoaicodeapp.domain.codegen.port;

import com.dango.dangoaicodeapp.domain.codegen.model.FeatureAnalysis;

/**
 * 功能分析领域端口，由基础设施层适配 AI 能力。
 */
public interface FeatureAnalysisPort {

    /**
     * 基于完整描述分析功能列表。
     */
    FeatureAnalysis analyzeFeatures(String fullPrompt);
}
