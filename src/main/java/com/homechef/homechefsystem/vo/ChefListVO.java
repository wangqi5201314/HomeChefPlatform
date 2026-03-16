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
public class ChefListVO {

    private Long id;

    private String name;

    private String avatar;

    private String specialtyCuisine;

    private Integer yearsOfExperience;

    private BigDecimal ratingAvg;

    private Integer orderCount;

    private Integer certStatus;

    private Integer status;
}
