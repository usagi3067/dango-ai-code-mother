package com.dango.dangoaicodeapp.domain.codegen.model;

/**
 * 领域层功能项值对象。
 */
public record AnalyzedFeature(String name, String description, boolean checked, boolean recommended) {

    public AnalyzedFeature {
        name = normalize(name, FeatureAnalysisConstraints.MAX_NAME_LENGTH,
                FeatureAnalysisConstraints.DEFAULT_FEATURE_NAME);
        description = normalize(description, FeatureAnalysisConstraints.MAX_DESCRIPTION_LENGTH,
                FeatureAnalysisConstraints.DEFAULT_FEATURE_DESCRIPTION);
    }

    private static String normalize(String value, int maxLength, String defaultValue) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.isEmpty()) {
            safeValue = defaultValue;
        }
        if (safeValue.length() > maxLength) {
            safeValue = safeValue.substring(0, maxLength);
        }
        return safeValue;
    }
}
