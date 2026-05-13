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
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
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
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
