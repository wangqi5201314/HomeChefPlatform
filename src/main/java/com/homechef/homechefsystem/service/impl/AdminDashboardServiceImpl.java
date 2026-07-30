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

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
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
