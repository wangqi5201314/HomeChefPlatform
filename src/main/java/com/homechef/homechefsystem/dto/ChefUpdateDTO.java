package com.homechef.homechefsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefUpdateDTO {

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

    /**
     * 服务模式：1=用户自备食材，2=平台协同采购，3=均支持
     */
    private Integer serviceMode;

    private Integer status;
}
