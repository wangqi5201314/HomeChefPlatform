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
public class PaymentStatusVO {

    private Long orderId;

    private String payNo;

    private BigDecimal payAmount;

    private String payStatus;

    private String transactionId;

    private LocalDateTime paidAt;

    private BigDecimal refundAmount;

    private String refundStatus;

    private LocalDateTime refundAt;
}
