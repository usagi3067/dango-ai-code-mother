package com.dango.dangoaicodemother.core;

import com.dango.dangoaicodemother.ai.AiAppInfoGeneratorService;
import com.dango.dangoaicodemother.ai.model.AppNameAndTagResult;
import com.dango.dangoaicodemother.model.enums.AppTagEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 应用信息生成门面实现类
 * 封装 AI 生成应用名称和标签的逻辑，处理异常和降级
 */
@Slf4j
@Service
public class AppInfoGeneratorFacadeImpl implements AppInfoGeneratorFacade {

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
    private AiAppInfoGeneratorService aiAppInfoGeneratorService;

    @Override
    public AppNameAndTagResult generateAppInfo(String initPrompt) {
        AppNameAndTagResult result = new AppNameAndTagResult();

        if (initPrompt == null || initPrompt.isBlank()) {
            log.warn("initPrompt 为空，返回默认值");
            result.setAppName("未命名应用");
            result.setTag(AppTagEnum.getDefaultText());
            return result;
        }

        try {
            AppNameAndTagResult aiResult = aiAppInfoGeneratorService.generateAppInfo(initPrompt);

            // 处理应用名称
            String appName = sanitizeName(aiResult.getAppName());
            if (appName == null || appName.isBlank()) {
                appName = fallbackName(initPrompt);
            }
            result.setAppName(appName);

            // 处理标签（验证有效性）
            String tag = aiResult.getTag();
            if (!AppTagEnum.isValidText(tag)) {
                log.warn("AI 返回无效标签: {}，使用默认标签", tag);
                tag = AppTagEnum.getDefaultText();
            }
            result.setTag(tag);

            return result;
        } catch (Exception e) {
            log.warn("AI 调用失败，使用降级值: {}", e.getMessage());
            result.setAppName(fallbackName(initPrompt));
            result.setTag(AppTagEnum.getDefaultText());
            return result;
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
    String sanitizeName(String name) {
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

    /**
     * 验证标签是否有效
     *
     * @param tag 标签值
     * @return 如果有效返回 true，否则返回 false
     */
    boolean isValidTag(String tag) {
        return AppTagEnum.isValidText(tag);
    }
}
