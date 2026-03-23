package com.homechef.homechefsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    DISABLED(0, "禁用"),
    NORMAL(1, "正常");

    private final Integer code;

    private final String desc;

    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        for (UserStatusEnum statusEnum : values()) {
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
        for (UserStatusEnum statusEnum : values()) {
            if (statusEnum.code.equals(code)) {
                return statusEnum.desc;
            }
        }
        return null;
    }
}
