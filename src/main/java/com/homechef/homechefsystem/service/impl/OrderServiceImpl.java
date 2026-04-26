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
     * 方法说明：根据用户提交的下单信息创建订单，并在同一事务中锁定对应档期。
     * 主要作用：这是用户端下单的核心方法，用来防止同一档期在并发请求下被重复占用，同时把订单主数据一次性落库。
     * 实现逻辑：方法会先规范化 timeSlot 并校验服务范围，然后通过 FOR UPDATE 锁住可预约档期；锁定成功后写入订单，再把档期更新为不可预约，任意一步失败都会随事务一起回滚。
     */
    @Override
    @Transactional
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

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 用户端订单服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public OrderDetailVO getById(Long id) {
        return toOrderDetailVO(orderMapper.selectById(id));
    }

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 用户端订单服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
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
     * 方法说明：取消指定订单，并在需要时释放该订单占用的厨师档期。
     * 主要作用：它用来处理用户主动取消订单的场景，确保订单状态变更后，之前被锁定的可预约资源能够重新开放。
     * 实现逻辑：方法会先查询订单并校验当前状态是否允许取消，再补齐取消原因、更新订单状态，最后按照 lockedOrderId 释放对应档期。
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
        // 用户取消待确认/待支付订单时，释放之前被该订单锁定的档期。
        chefScheduleMapper.releaseByLockedOrderId(id, LocalDateTime.now());
        return toOrderDetailVO(orderMapper.selectById(id));
    }

    /**
     * 方法说明：在 用户端订单服务实现 中处理 canCancelByUser 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    private boolean canCancelByUser(String orderStatus) {
        return OrderStatusEnum.PENDING_CONFIRM.equalsCode(orderStatus)
                || OrderStatusEnum.WAIT_PAY.equalsCode(orderStatus);
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 用户端订单服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 用户端订单服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
     * 方法说明：生成当前业务流程所需的编号、验证码或标识值。
     * 主要作用：它为 用户端订单服务实现 提供统一的标识生成能力，避免在主流程中混入随机数或格式拼接细节。
     * 实现逻辑：实现逻辑通常会结合时间、随机数或固定前缀构造结果，并确保生成值满足当前业务展示或唯一性需求。
     */
    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    /**
     * 方法说明：生成当前业务流程所需的编号、验证码或标识值。
     * 主要作用：它为 用户端订单服务实现 提供统一的标识生成能力，避免在主流程中混入随机数或格式拼接细节。
     * 实现逻辑：实现逻辑通常会结合时间、随机数或固定前缀构造结果，并确保生成值满足当前业务展示或唯一性需求。
     */
    private String generateConfirmCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    /**
     * 方法说明：对输入值做统一的格式化和规范化处理。
     * 主要作用：该方法用于消除 用户端订单服务实现 中大小写、空白字符或别名写法带来的差异，保证后续逻辑按统一格式处理数据。
     * 实现逻辑：实现时会先做空值判断，再进行 trim、大小写转换或枚举标准化，最终返回可直接参与业务判断的值。
     */
    private String normalizeTimeSlot(String timeSlot) {
        TimeSlotEnum timeSlotEnum = TimeSlotEnum.fromCode(timeSlot);
        if (timeSlotEnum == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, TimeSlotEnum.INVALID_MESSAGE);
        }
        return timeSlotEnum.getCode();
    }

    /**
     * 方法说明：锁定指定厨师在指定日期和时段下仍可预约的档期记录。
     * 主要作用：这个辅助方法用于控制并发下单，保证同一档期在同一时刻只会被一个事务成功占用。
     * 实现逻辑：实现时会按厨师、服务日期和时段查询可预约档期，并通过 FOR UPDATE 加行锁；如果未查到记录，则直接提示当前档期不可预约。
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
     * 方法说明：校验本次下单地址是否处于厨师当前服务半径范围内。
     * 主要作用：它负责在订单正式创建前拦截超出服务范围的请求，避免无效订单继续进入档期锁定和支付流程。
     * 实现逻辑：方法会依次加载用户地址、厨师信息和启用中的服务位置，检查经纬度及服务半径配置，再调用距离服务计算两点距离并与服务半径比较。
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
