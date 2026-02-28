package com.dango.dangoaicodeapp.domain.codegen.service;

import com.dango.dangoaicodeapp.domain.codegen.model.FeatureAnalysis;

/**
 * 功能分析领域服务。
 */
public interface FeatureAnalysisDomainService {

    /**
     * 根据用户描述分析功能列表。
     */
    FeatureAnalysis analyzeFeatures(String prompt, String supplement);
}
