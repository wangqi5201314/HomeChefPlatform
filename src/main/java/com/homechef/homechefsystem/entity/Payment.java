package com.homechef.homechefsystem.entity;

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
public class Payment {

    private Long id;

    private Long orderId;

    private String payNo;

    private String payChannel;

    private BigDecimal payAmount;

    /**
     * 支付状态：UNPAID=未支付，PAID=已支付
     */
    private String payStatus;

    private String transactionId;

    private LocalDateTime paidAt;

    private String refundNo;

    private BigDecimal refundAmount;

    /**
     * 退款状态：NONE=无退款，REFUNDED=已退款
     */
    private String refundStatus;

    private LocalDateTime refundAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
