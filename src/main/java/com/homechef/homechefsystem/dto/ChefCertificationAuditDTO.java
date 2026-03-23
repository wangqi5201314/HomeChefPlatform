package com.homechef.homechefsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefCertificationAuditDTO {

    /**
     * 审核状态：1=已通过，2=已拒绝
     */
    private Integer auditStatus;

    private String auditRemark;
}
