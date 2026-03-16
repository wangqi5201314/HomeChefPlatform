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

    private Integer serviceMode;

    private Integer status;
}
