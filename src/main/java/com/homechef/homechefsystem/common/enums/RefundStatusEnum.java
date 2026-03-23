package com.homechef.homechefsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundStatusEnum {

    NONE("NONE", "无退款"),
    REFUNDED("REFUNDED", "已退款");

    private final String code;

    private final String desc;

    public boolean equalsCode(String code) {
        return this.code.equals(code);
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        for (RefundStatusEnum statusEnum : values()) {
            if (statusEnum.equalsCode(code)) {
                return true;
            }
        }
        return false;
    }

    public static String getDescByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (RefundStatusEnum statusEnum : values()) {
            if (statusEnum.equalsCode(code)) {
                return statusEnum.desc;
            }
        }
        return null;
    }
}
