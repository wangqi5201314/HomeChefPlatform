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
public class AdminChefVO {

    private Long id;

    private String name;

    private String phone;

    private String specialtyCuisine;

    private Integer yearsOfExperience;

    private BigDecimal ratingAvg;

    private Integer certStatus;

    private String certStatusDesc;

    private Integer status;

    private LocalDateTime createdAt;
}
