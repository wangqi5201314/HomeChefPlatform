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

    /**
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
     */
    @Override
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

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public ChefOrderDetailVO getCurrentChefOrderDetail(Long id) {
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    /**
     * 接受一笔待处理的订单。
     * 这个方法主要给厨师接单使用，让订单从待确认进入下一步。
     * 它会先检查订单归属和状态，再把订单更新成已接单后的状态。
     */
    @Override
    public ChefOrderDetailVO accept(Long id) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, OrderStatusEnum.PENDING_CONFIRM, "待确认订单之外不能接单");
        updateOrderStatus(order, OrderStatusEnum.WAIT_PAY.getCode(), null);
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    /**
     * 拒绝一笔待处理的订单。
     * 这个方法主要给厨师无法接单时使用，同时还要把占用的档期释放掉。
     * 它会先检查订单归属和状态，再更新拒单状态，最后释放相关档期。
     */
    @Override
    @Transactional
    public ChefOrderDetailVO reject(Long id, ChefOrderRejectDTO chefOrderRejectDTO) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, OrderStatusEnum.PENDING_CONFIRM, "待确认订单之外不能拒单");
        updateOrderStatus(order, OrderStatusEnum.REJECTED.getCode(), chefOrderRejectDTO.getReason());
        chefScheduleMapper.releaseByLockedOrderId(order.getId(), LocalDateTime.now());
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    /**
     * 把订单或服务标记为开始执行。
     * 这个方法用于厨师真正开始上门服务时推进订单流程。
     * 它会先检查当前状态是否允许开始，然后再更新订单状态。
     */
    @Override
    public ChefOrderDetailVO start(Long id) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, OrderStatusEnum.PAID, "非已支付订单不能开始服务");
        updateOrderStatus(order, OrderStatusEnum.IN_SERVICE.getCode(), null);
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    /**
     * 把订单或服务标记为已经完成。
     * 这个方法用于厨师完成服务后结束整个履约流程。
     * 它会先检查订单当前状态，再把它更新为已完成。
     */
    @Override
    @Transactional
    public ChefOrderDetailVO finish(Long id) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, OrderStatusEnum.IN_SERVICE, "非服务中订单不能完成服务");
        LocalDateTime now = LocalDateTime.now();
        updateOrderStatus(order, OrderStatusEnum.COMPLETED.getCode(), null, now);
        int rows = chefMapper.incrementOrderCountById(order.getChefId(), now);
        if (rows <= 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "update chef order count failed");
        }
        chefScheduleMapper.clearLockedOrderIdByOrderId(order.getId(), now);
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    /**
     * 查出当前业务必须要用的数据。
     * 这个方法用于把“先查数据，找不到就报错”这类逻辑集中到一起。
     * 它会根据 id 或当前登录信息去查记录，如果查不到或不符合条件，就直接抛出异常。
     */
    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
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
     * 确认当前数据状态是否满足继续执行的要求。
     * 这个方法的作用，是把状态判断集中起来，避免主流程里到处写 if 判断。
     * 它会检查状态是否和预期一致，如果不一致，就直接抛出业务异常。
     */
    private void ensureOrderStatus(Order order, OrderStatusEnum expectedStatus, String message) {
        if (!expectedStatus.equalsCode(order.getOrderStatus())) {
            throw new BusinessException(ResultCodeEnum.FAIL, message);
        }
    }

    /**
     * 修改一条已有的业务数据。
     * 这个方法用于更新详情信息、状态或某些可编辑字段。
     * 它会先查出原始数据，再把新值写进去，最后保存并返回更新后的结果。
     */
    private void updateOrderStatus(Order order, String orderStatus, String reason) {
        updateOrderStatus(order, orderStatus, reason, LocalDateTime.now());
    }

    /**
     * 修改一条已有的业务数据。
     * 这个方法用于更新详情信息、状态或某些可编辑字段。
     * 它会先查出原始数据，再把新值写进去，最后保存并返回更新后的结果。
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
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
