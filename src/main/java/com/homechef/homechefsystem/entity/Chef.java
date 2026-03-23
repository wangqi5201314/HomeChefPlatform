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
public class Chef {

    private Long id;

    private String name;

    private String phone;

    private String password;

    private String avatar;

    private Integer gender;

    private Integer age;

    private String introduction;

    private String specialtyCuisine;

    private String specialtyTags;

    private Integer yearsOfExperience;

    private Integer serviceRadiusKm;

    /**
     * 服务模式：1=用户自备食材，2=平台协同采购，3=均支持
     */
    private Integer serviceMode;

    private BigDecimal ratingAvg;

    private Integer orderCount;

    private BigDecimal onTimeRate;

    private BigDecimal goodReviewRate;

    private Integer certStatus;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
