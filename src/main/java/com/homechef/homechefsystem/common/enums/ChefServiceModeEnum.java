package com.homechef.homechefsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChefServiceModeEnum {

    USER_PREPARES_INGREDIENTS(1, "用户自备食材"),
    PLATFORM_COORDINATED_PURCHASE(2, "平台协同采购"),
    BOTH_SUPPORTED(3, "均支持");

    private final Integer code;

    private final String desc;

    public static String getDescByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ChefServiceModeEnum modeEnum : values()) {
            if (modeEnum.code.equals(code)) {
                return modeEnum.desc;
            }
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        for (ChefServiceModeEnum modeEnum : values()) {
            if (modeEnum.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
