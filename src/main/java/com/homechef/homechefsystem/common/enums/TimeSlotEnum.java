package com.homechef.homechefsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TimeSlotEnum {

    BREAKFAST("BREAKFAST", "早餐"),
    LUNCH("LUNCH", "午餐"),
    DINNER("DINNER", "晚餐"),
    LATE_NIGHT("LATE_NIGHT", "夜宵");

    public static final String VALIDATION_REGEXP = "BREAKFAST|LUNCH|DINNER|LATE_NIGHT";
    public static final String INVALID_MESSAGE = "timeSlot 取值非法，只能为 BREAKFAST、LUNCH、DINNER、LATE_NIGHT";

    private final String code;

    private final String desc;

    public boolean equalsCode(String code) {
        return this.code.equals(code);
    }

    public static TimeSlotEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmedCode = code.trim();
        for (TimeSlotEnum timeSlotEnum : values()) {
            if (timeSlotEnum.equalsCode(trimmedCode)) {
                return timeSlotEnum;
            }
        }
        return null;
    }

    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }

    public static String getDescByCode(String code) {
        TimeSlotEnum timeSlotEnum = fromCode(code);
        return timeSlotEnum == null ? null : timeSlotEnum.getDesc();
    }
}
