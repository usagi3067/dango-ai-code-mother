package com.dango.dangoaicodeapp.domain.app.valueobject;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 操作模式枚举
 * 定义工作流的三种操作模式
 *
 * @author dango
 */
@Getter
public enum OperationModeEnum {

    CREATE("create", "创建模式"),
    MODIFY("modify", "修改模式"),
    FIX("fix", "修复模式");

    private final String value;
    private final String text;

    OperationModeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static OperationModeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (OperationModeEnum anEnum : OperationModeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
