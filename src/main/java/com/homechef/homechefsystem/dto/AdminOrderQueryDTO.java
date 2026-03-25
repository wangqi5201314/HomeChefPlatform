package com.homechef.homechefsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderQueryDTO {

    private String orderNo;

    private String orderStatus;

    private Long userId;

    private Long chefId;

    private LocalDate serviceDateFrom;

    private LocalDate serviceDateTo;

    private LocalDateTime createdAtFrom;

    private LocalDateTime createdAtTo;
}
