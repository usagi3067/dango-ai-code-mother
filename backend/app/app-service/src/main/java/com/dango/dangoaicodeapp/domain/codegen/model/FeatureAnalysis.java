package com.dango.dangoaicodeapp.domain.codegen.model;

import java.util.List;
import java.util.Objects;

/**
 * 领域层功能分析结果值对象。
 */
public record FeatureAnalysis(List<AnalyzedFeature> features) {
    public FeatureAnalysis {
        List<AnalyzedFeature> safeFeatures = features == null
                ? List.of()
                : features.stream()
                .filter(Objects::nonNull)
                .limit(FeatureAnalysisConstraints.MAX_FEATURE_COUNT)
                .toList();
        features = List.copyOf(safeFeatures);
    }

    public static FeatureAnalysis of(List<AnalyzedFeature> features) {
        return new FeatureAnalysis(features);
    }
}
