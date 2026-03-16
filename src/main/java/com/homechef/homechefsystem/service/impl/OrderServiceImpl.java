package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.dto.OrderCancelDTO;
import com.homechef.homechefsystem.dto.OrderCreateDTO;
import com.homechef.homechefsystem.dto.OrderQueryDTO;
import com.homechef.homechefsystem.entity.Order;
import com.homechef.homechefsystem.mapper.OrderMapper;
import com.homechef.homechefsystem.service.OrderService;
import com.homechef.homechefsystem.vo.OrderDetailVO;
import com.homechef.homechefsystem.vo.OrderListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    private static final String ORDER_STATUS_CANCELLED = "CANCELLED";

    private final OrderMapper orderMapper;

    @Override
    public OrderDetailVO createOrder(OrderCreateDTO orderCreateDTO) {
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .orderNo(generateOrderNo())
                .userId(orderCreateDTO.getUserId())
                .chefId(orderCreateDTO.getChefId())
                .addressId(orderCreateDTO.getAddressId())
                .serviceDate(orderCreateDTO.getServiceDate())
                .timeSlot(orderCreateDTO.getTimeSlot())
                .serviceStartTime(orderCreateDTO.getServiceStartTime())
                .serviceEndTime(orderCreateDTO.getServiceEndTime())
                .peopleCount(orderCreateDTO.getPeopleCount())
                .tastePreference(orderCreateDTO.getTastePreference())
                .tabooFood(orderCreateDTO.getTabooFood())
                .specialRequirement(orderCreateDTO.getSpecialRequirement())
                .ingredientMode(orderCreateDTO.getIngredientMode())
                .ingredientList(orderCreateDTO.getIngredientList())
                .contactName(orderCreateDTO.getContactName())
                .contactPhone(orderCreateDTO.getContactPhone())
                .fullAddress(orderCreateDTO.getFullAddress())
                .longitude(orderCreateDTO.getLongitude())
                .latitude(orderCreateDTO.getLatitude())
                .confirmCode(generateConfirmCode())
                .totalAmount(orderCreateDTO.getTotalAmount())
                .discountAmount(orderCreateDTO.getDiscountAmount())
                .payAmount(orderCreateDTO.getPayAmount())
                .orderStatus(ORDER_STATUS_PENDING_CONFIRM)
                .cancelReason(null)
                .refundReason(null)
                .userDeleted(0)
                .chefDeleted(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        int rows = orderMapper.insert(order);
        if (rows <= 0) {
            return null;
        }
        return toOrderDetailVO(orderMapper.selectById(order.getId()));
    }

    @Override
    public OrderDetailVO getById(Long id) {
        return toOrderDetailVO(orderMapper.selectById(id));
    }

    @Override
    public List<OrderListVO> getOrderList(OrderQueryDTO queryDTO) {
        List<Order> orderList = orderMapper.selectList(queryDTO);
        if (orderList == null || orderList.isEmpty()) {
            return Collections.emptyList();
        }
        return orderList.stream()
                .map(this::toOrderListVO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDetailVO cancelById(Long id, OrderCancelDTO orderCancelDTO) {
        Order existingOrder = orderMapper.selectById(id);
        if (existingOrder == null) {
            return null;
        }
        if (ORDER_STATUS_COMPLETED.equals(existingOrder.getOrderStatus())
                || ORDER_STATUS_CANCELLED.equals(existingOrder.getOrderStatus())) {
            return toOrderDetailVO(existingOrder);
        }

        int rows = orderMapper.cancelById(id, orderCancelDTO.getReason(), LocalDateTime.now());
        if (rows <= 0) {
            return null;
        }
        return toOrderDetailVO(orderMapper.selectById(id));
    }

    private OrderListVO toOrderListVO(Order order) {
        if (order == null) {
            return null;
        }
        return OrderListVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .chefId(order.getChefId())
                .serviceDate(order.getServiceDate())
                .timeSlot(order.getTimeSlot())
                .peopleCount(order.getPeopleCount())
                .totalAmount(order.getTotalAmount())
                .payAmount(order.getPayAmount())
                .orderStatus(order.getOrderStatus())
                .contactName(order.getContactName())
                .contactPhone(order.getContactPhone())
                .fullAddress(order.getFullAddress())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderDetailVO toOrderDetailVO(Order order) {
        if (order == null) {
            return null;
        }
        return OrderDetailVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .chefId(order.getChefId())
                .addressId(order.getAddressId())
                .serviceDate(order.getServiceDate())
                .timeSlot(order.getTimeSlot())
                .serviceStartTime(order.getServiceStartTime())
                .serviceEndTime(order.getServiceEndTime())
                .peopleCount(order.getPeopleCount())
                .tastePreference(order.getTastePreference())
                .tabooFood(order.getTabooFood())
                .specialRequirement(order.getSpecialRequirement())
                .ingredientMode(order.getIngredientMode())
                .ingredientList(order.getIngredientList())
                .contactName(order.getContactName())
                .contactPhone(order.getContactPhone())
                .fullAddress(order.getFullAddress())
                .longitude(order.getLongitude())
                .latitude(order.getLatitude())
                .confirmCode(order.getConfirmCode())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .payAmount(order.getPayAmount())
                .orderStatus(order.getOrderStatus())
                .cancelReason(order.getCancelReason())
                .refundReason(order.getRefundReason())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private String generateConfirmCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
