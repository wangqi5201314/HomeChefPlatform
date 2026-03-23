package com.homechef.homechefsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    PENDING_CONFIRM("PENDING_CONFIRM", "待厨师确认"),
    REJECTED("REJECTED", "厨师已拒单"),
    WAIT_PAY("WAIT_PAY", "待支付"),
    PAID("PAID", "已支付"),
    IN_SERVICE("IN_SERVICE", "服务中"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消"),
    REFUNDED("REFUNDED", "已退款");

    private final String code;

    private final String desc;

    public boolean equalsCode(String code) {
        return this.code.equals(code);
    }

    public static boolean isValid(String code) {
        for (OrderStatusEnum statusEnum : values()) {
            if (statusEnum.equalsCode(code)) {
                return true;
            }
        }
        return false;
    }
}
