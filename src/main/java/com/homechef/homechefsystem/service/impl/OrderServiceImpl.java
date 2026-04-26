package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.OrderStatusEnum;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.enums.TimeSlotEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.OrderCancelDTO;
import com.homechef.homechefsystem.dto.OrderCreateDTO;
import com.homechef.homechefsystem.dto.OrderQueryDTO;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.entity.ChefSchedule;
import com.homechef.homechefsystem.entity.ChefServiceLocation;
import com.homechef.homechefsystem.entity.Order;
import com.homechef.homechefsystem.entity.UserAddress;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.ChefScheduleMapper;
import com.homechef.homechefsystem.mapper.ChefServiceLocationMapper;
import com.homechef.homechefsystem.mapper.OrderMapper;
import com.homechef.homechefsystem.mapper.UserAddressMapper;
import com.homechef.homechefsystem.service.GeoDistanceService;
import com.homechef.homechefsystem.service.OrderService;
import com.homechef.homechefsystem.vo.OrderDetailVO;
import com.homechef.homechefsystem.vo.OrderListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final ChefScheduleMapper chefScheduleMapper;
    private final ChefServiceLocationMapper chefServiceLocationMapper;
    private final GeoDistanceService geoDistanceService;

    @Override
    @Transactional
    /**
     * 创建订单并在同一事务中锁定对应档期。
     */
    public OrderDetailVO createOrder(OrderCreateDTO orderCreateDTO) {
        LocalDateTime now = LocalDateTime.now();
        String timeSlot = normalizeTimeSlot(orderCreateDTO.getTimeSlot());
        // 下单前先做服务范围校验，避免超出厨师服务半径的订单进入后续锁档期流程。
        validateServiceRange(orderCreateDTO);
        // 这里会使用 SELECT ... FOR UPDATE 锁住对应档期行，防止并发请求同时抢到同一个档期。
        ChefSchedule lockedSchedule = lockAvailableSchedule(orderCreateDTO.getChefId(), orderCreateDTO.getServiceDate(), timeSlot);

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
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "create order failed");
        }

        // 订单创建成功后立即占用档期，并记录 locked_order_id，后续取消/拒单可通过订单ID释放档期。
        int scheduleRows = chefScheduleMapper.lockById(lockedSchedule.getId(), order.getId(), now);
        if (scheduleRows <= 0) {
            throw new BusinessException(ResultCodeEnum.FAIL, "当前档期已不可预约，请刷新后重试");
        }
        return toOrderDetailVO(orderMapper.selectById(order.getId()));
    }

    @Override
    /**
     * 根据 ID 查询对应数据。
     */
    public OrderDetailVO getById(Long id) {
        return toOrderDetailVO(orderMapper.selectById(id));
    }

    @Override
    /**
     * 查询列表数据并返回结果。
     */
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
    @Transactional
    /**
     * 取消指定订单并释放已锁定的档期。
     */
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
        // 用户取消待确认/待支付订单时，释放之前被该订单锁定的档期。
        chefScheduleMapper.releaseByLockedOrderId(id, LocalDateTime.now());
        return toOrderDetailVO(orderMapper.selectById(id));
    }

    /**
     * 处理 c an ca nc el by us er 相关逻辑。
     */
    private boolean canCancelByUser(String orderStatus) {
        return OrderStatusEnum.PENDING_CONFIRM.equalsCode(orderStatus)
                || OrderStatusEnum.WAIT_PAY.equalsCode(orderStatus);
    }

    /**
     * 将实体对象转换为前端返回 VO。
     */
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

    /**
     * 将实体对象转换为前端返回 VO。
     */
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

    /**
     * 生成业务订单编号。
     */
    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    /**
     * 生成订单确认码。
     */
    private String generateConfirmCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    /**
     * 标准化并校验时段枚举值。
     */
    private String normalizeTimeSlot(String timeSlot) {
        TimeSlotEnum timeSlotEnum = TimeSlotEnum.fromCode(timeSlot);
        if (timeSlotEnum == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, TimeSlotEnum.INVALID_MESSAGE);
        }
        return timeSlotEnum.getCode();
    }

    /**
     * 处理 l oc ka va il ab le sc he du le 相关逻辑。
     */
    private ChefSchedule lockAvailableSchedule(Long chefId, java.time.LocalDate serviceDate, String timeSlot) {
        // 当前方法必须在事务中调用，否则 MySQL 行锁会在查询结束后立刻释放，无法保护后续创建订单操作。
        ChefSchedule chefSchedule = chefScheduleMapper.selectAvailableByChefIdAndDateAndTimeSlotForUpdate(
                chefId,
                serviceDate,
                timeSlot
        );
        if (chefSchedule == null) {
            throw new BusinessException(ResultCodeEnum.FAIL, "当前档期不可预约");
        }
        return chefSchedule;
    }

    /**
     * 校验输入参数或业务状态是否合法。
     */
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

        // 优先使用腾讯地图导航距离，失败时 GeoDistanceService 内部会自动回退为 Haversine 直线距离。
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
