package com.dango.dangoaicodemother.core;

import com.dango.dangoaicodemother.ai.model.AppNameAndTagResult;

/**
 * 应用信息生成门面接口
 * 封装 AI 生成应用名称和标签的逻辑
 */
public interface AppInfoGeneratorFacade {

    /**
     * 生成应用名称和标签
     *
     * @param initPrompt 用户的初始描述
     * @return 应用名称和标签结果
     */
    AppNameAndTagResult generateAppInfo(String initPrompt);
}
