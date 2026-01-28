package com.dango.dangoaicodeapp.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 应用标签枚举
 * text: 英文标识
 * value: 中文描述
 */
@Getter
public enum AppTagEnum {

    TOOL("tool", "工具"),
    WEBSITE("website", "网站"),
    DATA_ANALYSIS("data_analysis", "数据分析"),
    ACTIVITY_PAGE("activity_page", "活动页面"),
    MANAGEMENT_PLATFORM("management_platform", "管理平台"),
    USER_APP("user_app", "用户应用"),
    PERSONAL_MANAGEMENT("personal_management", "个人管理"),
    GAME("game", "游戏");

    private final String text;
    private final String value;

    AppTagEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 text 获取枚举
     *
     * @param text 枚举的 text 值
     * @return 枚举值，如果未找到返回 null
     */
    public static AppTagEnum getEnumByText(String text) {
        if (ObjUtil.isEmpty(text)) {
            return null;
        }
        for (AppTagEnum anEnum : AppTagEnum.values()) {
            if (anEnum.text.equals(text)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 验证 text 是否有效
     *
     * @param text 要验证的 text 值
     * @return 如果 text 有效返回 true，否则返回 false
     */
    public static boolean isValidText(String text) {
        return getEnumByText(text) != null;
    }

    /**
     * 获取默认标签的 text
     *
     * @return 默认标签的 text 值
     */
    public static String getDefaultText() {
        return WEBSITE.getText();
    }
}
