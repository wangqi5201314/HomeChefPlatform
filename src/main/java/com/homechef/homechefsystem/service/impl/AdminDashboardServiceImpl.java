package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ChefCertStatusEnum;
import com.homechef.homechefsystem.common.enums.OrderStatusEnum;
import com.homechef.homechefsystem.mapper.ChefCertificationMapper;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.OrderMapper;
import com.homechef.homechefsystem.mapper.UserMapper;
import com.homechef.homechefsystem.service.AdminDashboardService;
import com.homechef.homechefsystem.vo.AdminDashboardOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserMapper userMapper;
    private final ChefMapper chefMapper;
    private final ChefCertificationMapper chefCertificationMapper;
    private final OrderMapper orderMapper;

    @Override
    /**
     * 汇总后台首页需要展示的概览统计信息。
     */
    public AdminDashboardOverviewVO getOverview() {
        LocalDate today = LocalDate.now();
        LocalDateTime startTime = today.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay();

        return AdminDashboardOverviewVO.builder()
                .userCount(userMapper.countAll())
                .chefCount(chefMapper.countAll())
                .pendingCertificationCount(chefCertificationMapper.countByAuditStatus(ChefCertStatusEnum.PENDING.getCode()))
                .orderCount(orderMapper.countAll())
                .todayOrderCount(orderMapper.countCreatedBetween(startTime, endTime))
                .pendingConfirmOrderCount(orderMapper.countByOrderStatus(OrderStatusEnum.PENDING_CONFIRM.getCode()))
                .inServiceOrderCount(orderMapper.countByOrderStatus(OrderStatusEnum.IN_SERVICE.getCode()))
                .build();
    }
}
