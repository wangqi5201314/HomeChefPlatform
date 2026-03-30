package com.homechef.homechefsystem.vo;

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
public class ChefServiceLocationVO {

    private Long id;

    private Long chefId;

    private String province;

    private String city;

    private String district;

    private String town;

    private String detailAddress;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
