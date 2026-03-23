package com.homechef.homechefsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefDetailVO {

    private Long id;

    private String name;

    private String phone;

    private String avatar;

    private Integer gender;

    private Integer age;

    private String introduction;

    private String specialtyCuisine;

    private String specialtyTags;

    private Integer yearsOfExperience;

    private Integer serviceRadiusKm;

    private Integer serviceMode;

    private String serviceModeDesc;

    private BigDecimal ratingAvg;

    private Integer orderCount;

    private BigDecimal onTimeRate;

    private BigDecimal goodReviewRate;

    private Integer certStatus;

    private String certStatusDesc;

    private Integer status;

    private String statusDesc;
}
