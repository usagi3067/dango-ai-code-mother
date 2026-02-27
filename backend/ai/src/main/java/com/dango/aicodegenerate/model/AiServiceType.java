package com.dango.aicodegenerate.model;

import lombok.Getter;

@Getter
public enum AiServiceType {
    CODE_GENERATOR("code-generator"),
    CODE_MODIFIER("code-modifier"),
    CODE_FIXER("code-fixer"),
    QA("qa"),
    ANIMATION_ADVISOR("animation-advisor"),
    MODIFICATION_PLANNER("modification-planner"),
    APP_INFO_GENERATOR("app-info-generator"),
    INTENT_CLASSIFIER("intent-classifier"),
    FEATURE_ANALYZER("feature-analyzer"),
    CODE_QUALITY_CHECK("code-quality-check"),
    IMAGE_COLLECTION("image-collection");

    private final String configKey;

    AiServiceType(String configKey) {
        this.configKey = configKey;
    }
}
