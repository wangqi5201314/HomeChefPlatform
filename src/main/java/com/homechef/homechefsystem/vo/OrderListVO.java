package com.homechef.homechefsystem.vo;

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
public class OrderListVO {

    private Long id;

    private String orderNo;

    private Long userId;

    private Long chefId;

    private LocalDate serviceDate;

    private String timeSlot;

    private Integer peopleCount;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private String orderStatus;

    private String contactName;

    private String contactPhone;

    private String fullAddress;

    private LocalDateTime createdAt;
}
