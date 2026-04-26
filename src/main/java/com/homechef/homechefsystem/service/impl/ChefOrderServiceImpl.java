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
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 厨师端订单服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
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
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师端订单服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public ChefOrderDetailVO getCurrentChefOrderDetail(Long id) {
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    /**
     * 方法说明：在 厨师端订单服务实现 中处理 accept 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
    public ChefOrderDetailVO accept(Long id) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, OrderStatusEnum.PENDING_CONFIRM, "待确认订单之外不能接单");
        updateOrderStatus(order, OrderStatusEnum.WAIT_PAY.getCode(), null);
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    /**
     * 方法说明：在 厨师端订单服务实现 中处理 reject 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
    @Transactional
    public ChefOrderDetailVO reject(Long id, ChefOrderRejectDTO chefOrderRejectDTO) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, OrderStatusEnum.PENDING_CONFIRM, "待确认订单之外不能拒单");
        updateOrderStatus(order, OrderStatusEnum.REJECTED.getCode(), chefOrderRejectDTO.getReason());
        // 厨师拒单后，之前下单时锁住的档期需要恢复为可预约，供其他用户继续选择。
        chefScheduleMapper.releaseByLockedOrderId(order.getId(), LocalDateTime.now());
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    /**
     * 方法说明：在 厨师端订单服务实现 中处理 start 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
    public ChefOrderDetailVO start(Long id) {
        Order order = getOwnedOrder(id);
        ensureOrderStatus(order, OrderStatusEnum.PAID, "非已支付订单不能开始服务");
        updateOrderStatus(order, OrderStatusEnum.IN_SERVICE.getCode(), null);
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    /**
     * 方法说明：在 厨师端订单服务实现 中处理 finish 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
        return toChefOrderDetailVO(getOwnedOrder(id));
    }

    /**
     * 方法说明：获取当前业务必需的数据，并在取不到时立即中断流程。
     * 主要作用：它把 厨师端订单服务实现 中“查询 + 非空校验”的重复套路合并成一个辅助方法，让主流程更聚焦业务本身。
     * 实现逻辑：实现时会先根据身份信息或业务键查询目标数据，再补充坐标、状态或归属校验，不满足条件时直接抛出业务异常。
     */
    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师端订单服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
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
     * 方法说明：确保当前业务状态满足继续执行的要求。
     * 主要作用：它用于把 厨师端订单服务实现 中必须成立的约束条件显式收口，避免非法状态继续向后流转。
     * 实现逻辑：实现时会读取当前对象的关键状态或字段，并与目标要求进行比较；若不满足则立即抛出业务异常。
     */
    private void ensureOrderStatus(Order order, OrderStatusEnum expectedStatus, String message) {
        if (!expectedStatus.equalsCode(order.getOrderStatus())) {
            throw new BusinessException(ResultCodeEnum.FAIL, message);
        }
    }

    /**
     * 方法说明：更新一条当前业务场景下的数据记录或状态。
     * 主要作用：该方法用于 厨师端订单服务实现 中的编辑、状态变更或流程推进，保证外部只能修改业务允许变动的部分。
     * 实现逻辑：实现时会先查询原始数据并做归属或状态校验，再回填可编辑字段执行更新，必要时返回更新后的详情结果。
     */
    private void updateOrderStatus(Order order, String orderStatus, String reason) {
        updateOrderStatus(order, orderStatus, reason, LocalDateTime.now());
    }

    /**
     * 方法说明：更新一条当前业务场景下的数据记录或状态。
     * 主要作用：该方法用于 厨师端订单服务实现 中的编辑、状态变更或流程推进，保证外部只能修改业务允许变动的部分。
     * 实现逻辑：实现时会先查询原始数据并做归属或状态校验，再回填可编辑字段执行更新，必要时返回更新后的详情结果。
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
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 厨师端订单服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 厨师端订单服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
