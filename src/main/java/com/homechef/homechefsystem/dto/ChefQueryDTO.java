package com.homechef.homechefsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefQueryDTO {

    private String name;

    private String specialtyCuisine;

    private Integer certStatus;

    private Integer status;
}
