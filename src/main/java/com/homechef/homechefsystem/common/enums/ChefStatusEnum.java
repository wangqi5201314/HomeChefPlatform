package com.homechef.homechefsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChefStatusEnum {

    DISABLED(0, "停用"),
    NORMAL(1, "正常");

    private final Integer code;

    private final String desc;

    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        for (ChefStatusEnum statusEnum : values()) {
            if (statusEnum.code.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static String getDescByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ChefStatusEnum statusEnum : values()) {
            if (statusEnum.code.equals(code)) {
                return statusEnum.desc;
            }
        }
        return null;
    }
}
