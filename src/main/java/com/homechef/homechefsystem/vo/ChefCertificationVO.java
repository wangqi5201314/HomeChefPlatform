package com.homechef.homechefsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefCertificationVO {

    private Long id;

    private Long chefId;

    private String realName;

    private String idCardNo;

    private String healthCertUrl;

    private String skillCertUrl;

    private String serviceCertUrl;

    private String advancedCertUrl;

    private Integer auditStatus;

    private String auditRemark;

    private LocalDateTime submittedAt;

    private LocalDateTime auditedAt;
}
