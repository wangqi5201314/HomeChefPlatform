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
public class UserAddress {

    private Long id;

    private Long userId;

    private String contactName;

    private String contactPhone;

    private String province;

    private String city;

    private String district;

    private String detailAddress;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String doorplate;

    private Integer isDefault;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
