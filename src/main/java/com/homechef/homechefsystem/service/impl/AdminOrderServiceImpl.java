package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.TimeSlotEnum;
import com.homechef.homechefsystem.dto.AdminOrderQueryDTO;
import com.homechef.homechefsystem.entity.Order;
import com.homechef.homechefsystem.mapper.OrderMapper;
import com.homechef.homechefsystem.service.AdminOrderService;
import com.homechef.homechefsystem.vo.AdminOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderMapper orderMapper;

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 后台订单查询服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
     */
    @Override
    public List<AdminOrderVO> getOrderList(AdminOrderQueryDTO queryDTO) {
        List<Order> orderList = orderMapper.selectAdminList(queryDTO);
        if (orderList == null || orderList.isEmpty()) {
            return Collections.emptyList();
        }
        return orderList.stream()
                .map(this::toAdminOrderVO)
                .collect(Collectors.toList());
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 后台订单查询服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
    private AdminOrderVO toAdminOrderVO(Order order) {
        if (order == null) {
            return null;
        }
        return AdminOrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .chefId(order.getChefId())
                .serviceDate(order.getServiceDate())
                .timeSlot(order.getTimeSlot())
                .timeSlotDesc(TimeSlotEnum.getDescByCode(order.getTimeSlot()))
                .peopleCount(order.getPeopleCount())
                .totalAmount(order.getTotalAmount())
                .payAmount(order.getPayAmount())
                .orderStatus(order.getOrderStatus())
                .contactName(order.getContactName())
                .contactPhone(order.getContactPhone())
                .fullAddress(order.getFullAddress())
                .reviewed(order.getReviewed())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
