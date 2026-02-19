package com.dango.dangoaicodeapp.domain.codegen.service;

import com.dango.aicodegenerate.model.FeatureAnalysisResult;
import com.dango.aicodegenerate.model.FeatureItem;
import com.dango.dangoaicodeapp.domain.codegen.ai.service.AiFeatureAnalyzerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FeatureAnalyzerFacadeImpl implements FeatureAnalyzerFacade {

    @Resource
    private AiFeatureAnalyzerService aiFeatureAnalyzerService;

    @Override
    public FeatureAnalysisResult analyzeFeatures(String prompt, String supplement) {
        String fullPrompt = prompt;
        if (supplement != null && !supplement.isBlank()) {
            fullPrompt = prompt + "\n\n补充说明：" + supplement;
        }

        try {
            FeatureAnalysisResult result = aiFeatureAnalyzerService.analyzeFeatures(fullPrompt);
            if (result.getFeatures() != null && result.getFeatures().size() > 6) {
                result.setFeatures(result.getFeatures().subList(0, 6));
            }
            return result;
        } catch (Exception e) {
            log.error("功能分析失败，返回默认结果", e);
            FeatureAnalysisResult fallback = new FeatureAnalysisResult();
            FeatureItem item = new FeatureItem();
            item.setName("核心功能");
            item.setDescription("根据描述实现主要功能");
            item.setChecked(true);
            item.setRecommended(true);
            fallback.setFeatures(List.of(item));
            return fallback;
        }
    }
}
