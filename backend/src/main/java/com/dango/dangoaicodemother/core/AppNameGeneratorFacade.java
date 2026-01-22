package com.dango.dangoaicodemother.core;

/**
 * 应用名称生成门面接口
 * 封装 AI 名称生成逻辑，提供统一的名称生成入口
 */
public interface AppNameGeneratorFacade {

    /**
     * 生成应用名称，失败时自动降级
     *
     * @param initPrompt 用户的初始描述
     * @return 应用名称
     */
    String generateAppName(String initPrompt);
}
