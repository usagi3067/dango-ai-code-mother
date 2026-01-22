package com.dango.dangoaicodemother.core;

import com.dango.dangoaicodemother.ai.AiAppNameGeneratorService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 应用名称生成门面实现类
 * 封装 AI 名称生成逻辑，处理异常和降级
 */
@Slf4j
@Service
public class AppNameGeneratorFacadeImpl implements AppNameGeneratorFacade {

    /**
     * 降级名称最大长度
     */
    private static final int FALLBACK_NAME_MAX_LENGTH = 12;

    /**
     * 生成名称最大长度
     */
    private static final int GENERATED_NAME_MAX_LENGTH = 20;

    /**
     * 特殊字符正则表达式
     */
    private static final String SPECIAL_CHARS_REGEX = "[\\n\\r@#$%^&*()\\[\\]{}|\\\\<>\"'`~;:+=]";

    @Resource
    private AiAppNameGeneratorService aiAppNameGeneratorService;

    @Override
    public String generateAppName(String initPrompt) {
        if (initPrompt == null || initPrompt.isBlank()) {
            log.warn("initPrompt 为空，返回默认名称");
            return "未命名应用";
        }

        try {
            String generatedName = aiAppNameGeneratorService.generateAppName(initPrompt);

            if (generatedName == null || generatedName.isBlank()) {
                log.warn("AI 返回空结果，使用降级名称");
                return fallbackName(initPrompt);
            }

            return sanitize(generatedName);
        } catch (Exception e) {
            log.warn("AI 调用失败，使用降级名称: {}", e.getMessage());
            return fallbackName(initPrompt);
        }
    }

    /**
     * 生成降级名称
     *
     * @param initPrompt 用户的初始描述
     * @return 降级名称（initPrompt 前 12 位）
     */
    String fallbackName(String initPrompt) {
        if (initPrompt == null || initPrompt.isBlank()) {
            return "未命名应用";
        }
        int length = Math.min(FALLBACK_NAME_MAX_LENGTH, initPrompt.length());
        return initPrompt.substring(0, length);
    }

    /**
     * 清理名称
     * - 去除首尾空白
     * - 去除特殊字符
     * - 截断超长名称
     *
     * @param name 原始名称
     * @return 清理后的名称
     */
    String sanitize(String name) {
        if (name == null) {
            return "";
        }

        // 去除首尾空白
        String sanitized = name.trim();

        // 去除特殊字符和换行符
        sanitized = sanitized.replaceAll(SPECIAL_CHARS_REGEX, "");

        // 截断超长名称
        if (sanitized.length() > GENERATED_NAME_MAX_LENGTH) {
            sanitized = sanitized.substring(0, GENERATED_NAME_MAX_LENGTH);
        }

        return sanitized;
    }
}
