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
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 后台数据看板服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
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
