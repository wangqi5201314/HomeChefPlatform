package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.vo.AdminDashboardOverviewVO;

public interface AdminDashboardService {

    /**
     * 获取后台首页的概览统计信息。
     */
    AdminDashboardOverviewVO getOverview();
}
