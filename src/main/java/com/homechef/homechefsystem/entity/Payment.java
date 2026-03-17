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

    private String payStatus;

    private String transactionId;

    private LocalDateTime paidAt;

    private String refundNo;

    private BigDecimal refundAmount;

    private String refundStatus;

    private LocalDateTime refundAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
