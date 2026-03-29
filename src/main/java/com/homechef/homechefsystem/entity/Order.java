package com.homechef.homechefsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private Long id;

    private String orderNo;

    private Long userId;

    private Long chefId;

    private Long addressId;

    private LocalDate serviceDate;

    /**
     * 时段：BREAKFAST / LUNCH / DINNER / LATE_NIGHT
     */
    private String timeSlot;

    private LocalDateTime serviceStartTime;

    private LocalDateTime serviceEndTime;

    private Integer peopleCount;

    private String tastePreference;

    private String tabooFood;

    private String specialRequirement;

    private Integer ingredientMode;

    private String ingredientList;

    private String contactName;

    private String contactPhone;

    private String fullAddress;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String confirmCode;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    private String orderStatus;

    private String cancelReason;

    private String refundReason;

    private Integer userDeleted;

    private Integer chefDeleted;

    private Integer reviewed;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
