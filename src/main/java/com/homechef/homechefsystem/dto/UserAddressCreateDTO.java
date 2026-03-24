package com.homechef.homechefsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressCreateDTO {

    private Long userId;

    private String contactName;

    private String contactPhone;

    private String province;

    private String city;

    private String district;

    private String town;

    private String detailAddress;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer isDefault;
}
