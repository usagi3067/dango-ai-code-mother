package com.dango.dangoaicodeapp.application.service;

import com.dango.dangoaicodeapp.model.vo.FeatureAnalysisVO;

/**
 * 功能分析应用服务。
 */
public interface FeatureAnalysisApplicationService {

    /**
     * 分析并返回功能列表。
     */
    FeatureAnalysisVO analyzeFeatures(String prompt, String supplement);
}
