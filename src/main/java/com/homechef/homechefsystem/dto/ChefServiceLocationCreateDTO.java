package com.homechef.homechefsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefServiceLocationCreateDTO {

    private String locationName;

    @NotBlank(message = "province 不能为空")
    private String province;

    @NotBlank(message = "city 不能为空")
    private String city;

    @NotBlank(message = "district 不能为空")
    private String district;

    private String town;

    @NotBlank(message = "detailAddress 不能为空")
    private String detailAddress;

    @NotNull(message = "longitude 不能为空")
    private BigDecimal longitude;

    @NotNull(message = "latitude 不能为空")
    private BigDecimal latitude;
}
