package com.dango.dangoaicodeapp.domain.codegen.model;

/**
 * 功能分析约束常量。
 */
public final class FeatureAnalysisConstraints {

    private FeatureAnalysisConstraints() {
    }

    public static final int MAX_FEATURE_COUNT = 6;
    public static final int MAX_PROMPT_LENGTH = 2000;
    public static final int MAX_SUPPLEMENT_LENGTH = 2000;
    public static final int MAX_NAME_LENGTH = 10;
    public static final int MAX_DESCRIPTION_LENGTH = 30;
    public static final String DEFAULT_FEATURE_NAME = "未命名功能";
    public static final String DEFAULT_FEATURE_DESCRIPTION = "功能描述待完善";
}
