package com.dango.dangoaicodeapp.infrastructure.ai.gateway;

import com.dango.dangoaicodeapp.domain.codegen.model.AnalyzedFeature;
import com.dango.dangoaicodeapp.domain.codegen.model.FeatureAnalysis;
import com.dango.dangoaicodeapp.domain.codegen.port.FeatureAnalysisPort;
import com.dango.dangoaicodeapp.infrastructure.ai.model.FeatureAnalysisAiResult;
import com.dango.dangoaicodeapp.infrastructure.ai.model.FeatureItemAiResult;
import com.dango.dangoaicodeapp.infrastructure.ai.service.AiFeatureAnalyzerService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class FeatureAnalysisPortImpl implements FeatureAnalysisPort {

    @Resource
    private AiFeatureAnalyzerService aiFeatureAnalyzerService;

    @Override
    public FeatureAnalysis analyzeFeatures(String fullPrompt) {
        FeatureAnalysisAiResult result = aiFeatureAnalyzerService.analyzeFeatures(fullPrompt);
        return toDomainFeatureAnalysis(result);
    }

    private FeatureAnalysis toDomainFeatureAnalysis(FeatureAnalysisAiResult result) {
        if (result == null || result.getFeatures() == null) {
            return FeatureAnalysis.of(List.of());
        }

        List<AnalyzedFeature> features = result.getFeatures().stream()
                .filter(Objects::nonNull)
                .map(this::toAnalyzedFeature)
                .toList();
        return FeatureAnalysis.of(features);
    }

    private AnalyzedFeature toAnalyzedFeature(FeatureItemAiResult item) {
        return new AnalyzedFeature(
                item.getName(),
                item.getDescription(),
                item.isChecked(),
                item.isRecommended()
        );
    }
}
