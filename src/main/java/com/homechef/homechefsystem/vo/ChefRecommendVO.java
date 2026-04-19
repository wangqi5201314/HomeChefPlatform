package com.homechef.homechefsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefRecommendVO {

    private Long id;

    private String name;

    private String avatar;

    private String specialtyCuisine;

    private Integer yearsOfExperience;

    private BigDecimal ratingAvg;

    private Integer orderCount;

    private BigDecimal goodReviewRate;

    private Integer serviceMode;

    private String serviceModeDesc;

    private Integer serviceRadiusKm;

    private String serviceAreaText;

    private BigDecimal distanceKm;

    private LocalDate nearestAvailableDate;

    private String nearestAvailableTimeSlot;

    private String nearestAvailableTimeSlotDesc;
}
