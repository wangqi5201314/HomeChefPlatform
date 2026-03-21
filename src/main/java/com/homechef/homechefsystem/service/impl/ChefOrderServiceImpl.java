package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.ChefOrderRejectDTO;
import com.homechef.homechefsystem.entity.Order;
import com.homechef.homechefsystem.mapper.OrderMapper;
import com.homechef.homechefsystem.service.ChefOrderService;
import com.homechef.homechefsystem.utils.LoginUserContext;
import com.homechef.homechefsystem.vo.ChefOrderDetailVO;
import com.homechef.homechefsystem.vo.ChefOrderListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChefOrderServiceImpl implements ChefOrderService {

    private static final String ORDER_STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String ORDER_STATUS_WAIT_PAY = "WAIT_PAY";
    private static final String ORDER_STATUS_PAID = "PAID";
    private static final String ORDER_STATUS_IN_SERVICE = "IN_SERVICE";
    private static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    private static final String ORDER_STATUS_CANCELLED = "CANCELLED";

    private final OrderMapper orderMapper;

    @Override
    public List<ChefOrderListVO> getCurrentChefOrderList(String orderStatus) {
        Long chefId = requireCurrentChefId();
        List<Order> orderList = orderMapper.selectChefList(chefId, orderStatus);
        if (orderList == null || orderList.isEmpty()) {
            return Collections.emptyList();
        }
        return orderList.stream()
                .map(this::toChefOrderListVO)
                .collect(Collectors.toList());
    }

    @Override
    public ChefOrderDetailVO getCurrentChefOrderDetail(Long id) {
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    @Override
    public ChefOrderDetailVO accept(Long id) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, ORDER_STATUS_PENDING_CONFIRM, "order status does not allow accept");
        updateOrderStatus(order, ORDER_STATUS_WAIT_PAY, null);
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    @Override
    public ChefOrderDetailVO reject(Long id, ChefOrderRejectDTO chefOrderRejectDTO) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, ORDER_STATUS_PENDING_CONFIRM, "order status does not allow reject");
        updateOrderStatus(order, ORDER_STATUS_CANCELLED, chefOrderRejectDTO.getReason());
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    @Override
    public ChefOrderDetailVO start(Long id) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, ORDER_STATUS_PAID, "order status does not allow start service");
        updateOrderStatus(order, ORDER_STATUS_IN_SERVICE, null);
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    @Override
    public ChefOrderDetailVO finish(Long id) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, ORDER_STATUS_IN_SERVICE, "order status does not allow finish service");
        updateOrderStatus(order, ORDER_STATUS_COMPLETED, null);
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    private Order getOwnedOrder(Long id) {
        Long chefId = requireCurrentChefId();
        Order order = orderMapper.selectByIdAndChefId(id, chefId);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "order not found");
        }
        return order;
    }

    private void ensureOrderStatus(Order order, String expectedStatus, String message) {
        if (!expectedStatus.equals(order.getOrderStatus())) {
            throw new BusinessException(ResultCodeEnum.FAIL, message);
        }
    }

    private void updateOrderStatus(Order order, String orderStatus, String reason) {
        int rows;
        if (StringUtils.hasText(reason)) {
            rows = orderMapper.updateStatusAndCancelReasonById(order.getId(), order.getChefId(), orderStatus, reason, LocalDateTime.now());
        } else {
            rows = orderMapper.updateStatusByIdAndChefId(order.getId(), order.getChefId(), orderStatus, LocalDateTime.now());
        }
        if (rows <= 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "update order status failed");
        }
    }

    private ChefOrderListVO toChefOrderListVO(Order order) {
        if (order == null) {
            return null;
        }
        return ChefOrderListVO.builder()
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

    private ChefOrderDetailVO toChefOrderDetailVO(Order order) {
        if (order == null) {
            return null;
        }
        return ChefOrderDetailVO.builder()
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
}
