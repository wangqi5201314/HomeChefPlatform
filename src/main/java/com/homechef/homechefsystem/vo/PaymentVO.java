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
public class PaymentVO {

    private Long id;

    private Long orderId;

    private String payNo;

    private String payChannel;

    private BigDecimal payAmount;

    private String payStatus;

    private LocalDateTime createdAt;
}
