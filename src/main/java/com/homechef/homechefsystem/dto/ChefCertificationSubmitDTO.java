package com.homechef.homechefsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefCertificationSubmitDTO {

    private Long chefId;

    private String realName;

    private String idCardNo;

    private String healthCertUrl;

    private String skillCertUrl;

    private String serviceCertUrl;

    private String advancedCertUrl;
}
