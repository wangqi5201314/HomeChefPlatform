package com.homechef.homechefsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentVO {

    private Long id;

    private Long orderId;

    private String payNo;

    private BigDecimal payAmount;

    private String payStatus;

    private String payStatusDesc;

    private BigDecimal refundAmount;

    private String refundStatus;

    private String refundStatusDesc;

    private LocalDateTime createdAt;
}
