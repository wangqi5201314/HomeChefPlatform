package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.OrderStatusEnum;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.enums.TimeSlotEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.OrderCancelDTO;
import com.homechef.homechefsystem.dto.OrderCreateDTO;
import com.homechef.homechefsystem.dto.OrderQueryDTO;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.entity.ChefServiceLocation;
import com.homechef.homechefsystem.entity.Order;
import com.homechef.homechefsystem.entity.UserAddress;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.ChefServiceLocationMapper;
import com.homechef.homechefsystem.mapper.OrderMapper;
import com.homechef.homechefsystem.mapper.UserAddressMapper;
import com.homechef.homechefsystem.service.GeoDistanceService;
import com.homechef.homechefsystem.service.OrderService;
import com.homechef.homechefsystem.vo.OrderDetailVO;
import com.homechef.homechefsystem.vo.OrderListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final UserAddressMapper userAddressMapper;
    private final ChefMapper chefMapper;
    private final ChefServiceLocationMapper chefServiceLocationMapper;
    private final GeoDistanceService geoDistanceService;

    @Override
    public OrderDetailVO createOrder(OrderCreateDTO orderCreateDTO) {
        LocalDateTime now = LocalDateTime.now();
        String timeSlot = normalizeTimeSlot(orderCreateDTO.getTimeSlot());
        validateServiceRange(orderCreateDTO);
        Order order = Order.builder()
                .orderNo(generateOrderNo())
                .userId(orderCreateDTO.getUserId())
                .chefId(orderCreateDTO.getChefId())
                .addressId(orderCreateDTO.getAddressId())
                .serviceDate(orderCreateDTO.getServiceDate())
                .timeSlot(timeSlot)
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
                .orderStatus(OrderStatusEnum.PENDING_CONFIRM.getCode())
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
        if (!canCancelByUser(existingOrder.getOrderStatus())) {
            throw new BusinessException(ResultCodeEnum.FAIL, "当前订单状态不允许取消");
        }

        String cancelReason = orderCancelDTO == null ? null : orderCancelDTO.getReason();
        if (!StringUtils.hasText(cancelReason)) {
            cancelReason = "用户取消订单";
        }

        int rows = orderMapper.cancelById(
                id,
                OrderStatusEnum.CANCELLED.getCode(),
                cancelReason,
                LocalDateTime.now()
        );
        if (rows <= 0) {
            return null;
        }
        return toOrderDetailVO(orderMapper.selectById(id));
    }

    private boolean canCancelByUser(String orderStatus) {
        return OrderStatusEnum.PENDING_CONFIRM.equalsCode(orderStatus)
                || OrderStatusEnum.WAIT_PAY.equalsCode(orderStatus);
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
                .reviewed(order.getReviewed())
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

    private String normalizeTimeSlot(String timeSlot) {
        TimeSlotEnum timeSlotEnum = TimeSlotEnum.fromCode(timeSlot);
        if (timeSlotEnum == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, TimeSlotEnum.INVALID_MESSAGE);
        }
        return timeSlotEnum.getCode();
    }

    private void validateServiceRange(OrderCreateDTO orderCreateDTO) {
        UserAddress userAddress = userAddressMapper.selectById(orderCreateDTO.getAddressId());
        if (userAddress == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "收货地址不存在");
        }
        if (userAddress.getLongitude() == null || userAddress.getLatitude() == null) {
            throw new BusinessException(ResultCodeEnum.FAIL, "收货地址缺少坐标信息，无法校验服务范围");
        }

        Chef chef = chefMapper.selectById(orderCreateDTO.getChefId());
        if (chef == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "厨师不存在");
        }
        if (chef.getServiceRadiusKm() == null || chef.getServiceRadiusKm() <= 0) {
            throw new BusinessException(ResultCodeEnum.FAIL, "厨师未配置服务半径");
        }

        ChefServiceLocation chefServiceLocation = chefServiceLocationMapper.selectActiveByChefId(orderCreateDTO.getChefId());
        if (chefServiceLocation == null) {
            throw new BusinessException(ResultCodeEnum.FAIL, "厨师未设置启用中的服务位置");
        }
        if (chefServiceLocation.getLongitude() == null || chefServiceLocation.getLatitude() == null) {
            throw new BusinessException(ResultCodeEnum.FAIL, "厨师启用中的服务位置缺少坐标信息");
        }

        double distanceKm = geoDistanceService.distanceKm(
                userAddress.getLatitude(),
                userAddress.getLongitude(),
                chefServiceLocation.getLatitude(),
                chefServiceLocation.getLongitude()
        );
        double serviceRadiusKm = chef.getServiceRadiusKm().doubleValue();
        if (distanceKm > serviceRadiusKm) {
            throw new BusinessException(
                    ResultCodeEnum.FAIL,
                    String.format(
                            Locale.ROOT,
                            "当前地址距离厨师 %.2f 公里，超出其服务半径 %.2f 公里",
                            distanceKm,
                            serviceRadiusKm
                    )
            );
        }
    }
}
