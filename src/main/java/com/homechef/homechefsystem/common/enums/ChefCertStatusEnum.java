package com.homechef.homechefsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChefCertStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final Integer code;

    private final String desc;

    public static String getDescByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ChefCertStatusEnum statusEnum : values()) {
            if (statusEnum.code.equals(code)) {
                return statusEnum.desc;
            }
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        for (ChefCertStatusEnum statusEnum : values()) {
            if (statusEnum.code.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAuditResult(Integer code) {
        return APPROVED.code.equals(code) || REJECTED.code.equals(code);
    }
}
