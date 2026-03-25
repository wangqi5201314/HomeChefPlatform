package com.homechef.homechefsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardOverviewVO {

    private Integer userCount;

    private Integer chefCount;

    private Integer pendingCertificationCount;

    private Integer orderCount;

    private Integer todayOrderCount;

    private Integer pendingConfirmOrderCount;

    private Integer inServiceOrderCount;
}
