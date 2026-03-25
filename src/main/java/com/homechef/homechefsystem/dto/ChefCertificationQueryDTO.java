package com.homechef.homechefsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefCertificationQueryDTO {

    private Integer auditStatus;

    private Long chefId;

    private String realName;
}
