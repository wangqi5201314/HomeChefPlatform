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

    /**
     * 根据用户提交的信息创建一笔新订单。
     * 这个方法是下单流程的核心入口，负责避免并发重复下单、写入订单数据，并占用对应档期。
     * 它会先检查服务范围和时段是否合法，然后加锁可用档期，接着写入订单，最后把档期更新为不可预约。
     */
    @Override
    @Transactional
    public OrderDetailVO createOrder(OrderCreateDTO orderCreateDTO) {
        LocalDateTime now = LocalDateTime.now();
        String timeSlot = normalizeTimeSlot(orderCreateDTO.getTimeSlot());
        validateServiceRange(orderCreateDTO);
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

        int scheduleRows = chefScheduleMapper.lockById(lockedSchedule.getId(), order.getId(), now);
        if (scheduleRows <= 0) {
            throw new BusinessException(ResultCodeEnum.FAIL, "当前档期已不可预约，请刷新后重试");
        }
        return toOrderDetailVO(orderMapper.selectById(order.getId()));
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public OrderDetailVO getById(Long id) {
        return toOrderDetailVO(orderMapper.selectById(id));
    }

    /**
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
     */
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

    /**
     * 取消一笔指定的订单。
     * 这个方法主要用在用户不想继续订单时，同时把之前占用的档期释放回去。
     * 它会先查询订单状态是否允许取消，再写入取消原因并更新状态，最后释放该订单锁定的档期。
     */
    @Override
    @Transactional
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
        chefScheduleMapper.releaseByLockedOrderId(id, LocalDateTime.now());
        return toOrderDetailVO(orderMapper.selectById(id));
    }

    /**
     * 处理 canCancelByUser 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
    private boolean canCancelByUser(String orderStatus) {
        return OrderStatusEnum.PENDING_CONFIRM.equalsCode(orderStatus)
                || OrderStatusEnum.WAIT_PAY.equalsCode(orderStatus);
    }

    /**
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
     * 生成一个新的业务标识或编号。
     * 这个方法主要用来生成订单号、支付号、验证码这类不能手写的值。
     * 它会按固定规则把时间、随机数或前缀拼起来，最后返回一个可直接使用的新值。
     */
    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    /**
     * 生成一个新的业务标识或编号。
     * 这个方法主要用来生成订单号、支付号、验证码这类不能手写的值。
     * 它会按固定规则把时间、随机数或前缀拼起来，最后返回一个可直接使用的新值。
     */
    private String generateConfirmCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    /**
     * 把输入值整理成统一的格式。
     * 这个方法可以避免因为大小写、空格或不同写法导致后面的业务判断出错。
     * 它通常会先做 trim，再统一大小写，或者转成系统里约定好的标准值。
     */
    private String normalizeTimeSlot(String timeSlot) {
        TimeSlotEnum timeSlotEnum = TimeSlotEnum.fromCode(timeSlot);
        if (timeSlotEnum == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, TimeSlotEnum.INVALID_MESSAGE);
        }
        return timeSlotEnum.getCode();
    }

    /**
     * 锁住一条当前仍然可以预约的档期记录。
     * 这个方法的作用，是防止多个人同时抢到同一个厨师档期。
     * 它会按厨师、日期和时段去查可用档期，并通过数据库行锁把这条记录锁住。
     */
    private ChefSchedule lockAvailableSchedule(Long chefId, java.time.LocalDate serviceDate, String timeSlot) {
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
     * 检查用户的地址是否在厨师的服务范围内。
     * 这个方法用来避免用户下单后才发现距离过远，提前把不符合条件的请求拦住。
     * 它会分别查用户地址、厨师信息和启用中的服务位置，然后计算距离，再与服务半径做比较。
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
