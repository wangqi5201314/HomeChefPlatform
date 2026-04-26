package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.OrderStatusEnum;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.enums.TimeSlotEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.ChefOrderRejectDTO;
import com.homechef.homechefsystem.entity.Order;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.ChefScheduleMapper;
import com.homechef.homechefsystem.mapper.OrderMapper;
import com.homechef.homechefsystem.service.ChefOrderService;
import com.homechef.homechefsystem.utils.LoginUserContext;
import com.homechef.homechefsystem.vo.ChefOrderDetailVO;
import com.homechef.homechefsystem.vo.ChefOrderListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChefOrderServiceImpl implements ChefOrderService {

    private final OrderMapper orderMapper;
    private final ChefMapper chefMapper;
    private final ChefScheduleMapper chefScheduleMapper;

    @Override
    /**
     * 获取当前登录厨师的订单列表。
     */
    public List<ChefOrderListVO> getCurrentChefOrderList(String orderStatus) {
        Long chefId = requireCurrentChefId();
        if (StringUtils.hasText(orderStatus) && !OrderStatusEnum.isValid(orderStatus)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "订单状态不合法");
        }

        List<Order> orderList = orderMapper.selectChefList(chefId, orderStatus);
        if (orderList == null || orderList.isEmpty()) {
            return Collections.emptyList();
        }
        return orderList.stream()
                .map(this::toChefOrderListVO)
                .collect(Collectors.toList());
    }

    @Override
    /**
     * 获取当前登录厨师可查看的订单详情。
     */
    public ChefOrderDetailVO getCurrentChefOrderDetail(Long id) {
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    @Override
    /**
     * 执行接单处理。
     */
    public ChefOrderDetailVO accept(Long id) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, OrderStatusEnum.PENDING_CONFIRM, "待确认订单之外不能接单");
        updateOrderStatus(order, OrderStatusEnum.WAIT_PAY.getCode(), null);
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    @Override
    @Transactional
    /**
     * 执行拒单处理。
     */
    public ChefOrderDetailVO reject(Long id, ChefOrderRejectDTO chefOrderRejectDTO) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, OrderStatusEnum.PENDING_CONFIRM, "待确认订单之外不能拒单");
        updateOrderStatus(order, OrderStatusEnum.REJECTED.getCode(), chefOrderRejectDTO.getReason());
        // 厨师拒单后，之前下单时锁住的档期需要恢复为可预约，供其他用户继续选择。
        chefScheduleMapper.releaseByLockedOrderId(order.getId(), LocalDateTime.now());
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    @Override
    /**
     * 执行开始服务处理。
     */
    public ChefOrderDetailVO start(Long id) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, OrderStatusEnum.PAID, "非已支付订单不能开始服务");
        updateOrderStatus(order, OrderStatusEnum.IN_SERVICE.getCode(), null);
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    @Override
    @Transactional
    /**
     * 执行完成处理。
     */
    public ChefOrderDetailVO finish(Long id) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, OrderStatusEnum.IN_SERVICE, "非服务中订单不能完成服务");
        LocalDateTime now = LocalDateTime.now();
        updateOrderStatus(order, OrderStatusEnum.COMPLETED.getCode(), null, now);
        int rows = chefMapper.incrementOrderCountById(order.getChefId(), now);
        if (rows <= 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "update chef order count failed");
        }
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    /**
     * 获取并校验当前登录厨师的 ID。
     */
    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    /**
     * 处理 g et ow ne do rd er 相关逻辑。
     */
    private Order getOwnedOrder(Long id) {
        Long chefId = requireCurrentChefId();
        Order order = orderMapper.selectByIdAndChefId(id, chefId);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "order not found");
        }
        return order;
    }

    /**
     * 校验并确保当前业务条件成立。
     */
    private void ensureOrderStatus(Order order, OrderStatusEnum expectedStatus, String message) {
        if (!expectedStatus.equalsCode(order.getOrderStatus())) {
            throw new BusinessException(ResultCodeEnum.FAIL, message);
        }
    }

    /**
     * 处理 u pd at eo rd er st at us 相关逻辑。
     */
    private void updateOrderStatus(Order order, String orderStatus, String reason) {
        updateOrderStatus(order, orderStatus, reason, LocalDateTime.now());
    }

    /**
     * 处理 u pd at eo rd er st at us 相关逻辑。
     */
    private void updateOrderStatus(Order order, String orderStatus, String reason, LocalDateTime updatedAt) {
        int rows;
        if (StringUtils.hasText(reason)) {
            rows = orderMapper.updateStatusAndCancelReasonById(order.getId(), order.getChefId(), orderStatus, reason, updatedAt);
        } else {
            rows = orderMapper.updateStatusByIdAndChefId(order.getId(), order.getChefId(), orderStatus, updatedAt);
        }
        if (rows <= 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "update order status failed");
        }
    }

    /**
     * 将实体对象转换为前端返回 VO。
     */
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
                .timeSlotDesc(TimeSlotEnum.getDescByCode(order.getTimeSlot()))
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

    /**
     * 将实体对象转换为前端返回 VO。
     */
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
                .timeSlotDesc(TimeSlotEnum.getDescByCode(order.getTimeSlot()))
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
