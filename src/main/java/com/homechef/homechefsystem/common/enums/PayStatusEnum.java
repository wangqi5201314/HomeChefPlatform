package com.homechef.homechefsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PayStatusEnum {

    UNPAID("UNPAID", "未支付"),
    PAID("PAID", "已支付");

    private final String code;

    private final String desc;

    public boolean equalsCode(String code) {
        return this.code.equals(code);
    }

    public static boolean isValid(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        for (PayStatusEnum statusEnum : values()) {
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
        for (PayStatusEnum statusEnum : values()) {
            if (statusEnum.equalsCode(code)) {
                return statusEnum.desc;
            }
        }
        return null;
    }
}
