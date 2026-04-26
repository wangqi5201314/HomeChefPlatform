package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.PayStatusEnum;
import com.homechef.homechefsystem.common.enums.RefundStatusEnum;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.AdminPaymentQueryDTO;
import com.homechef.homechefsystem.entity.Payment;
import com.homechef.homechefsystem.mapper.PaymentMapper;
import com.homechef.homechefsystem.service.AdminPaymentService;
import com.homechef.homechefsystem.vo.AdminPaymentDetailVO;
import com.homechef.homechefsystem.vo.AdminPaymentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private final PaymentMapper paymentMapper;

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 后台支付管理服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public AdminPaymentDetailVO getPaymentDetailByOrderId(Long orderId) {
        return toAdminPaymentDetailVO(paymentMapper.selectByOrderId(orderId));
    }

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 后台支付管理服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
     */
    @Override
    public List<AdminPaymentVO> getPaymentList(AdminPaymentQueryDTO queryDTO) {
        validateQuery(queryDTO);

        List<Payment> paymentList = paymentMapper.selectAdminList(queryDTO);
        if (paymentList == null || paymentList.isEmpty()) {
            return Collections.emptyList();
        }
        return paymentList.stream()
                .map(this::toAdminPaymentVO)
                .collect(Collectors.toList());
    }

    /**
     * 方法说明：校验当前业务输入或状态是否满足执行条件。
     * 主要作用：它用于把 后台支付管理服务实现 中的前置规则集中收口，避免核心流程夹杂过多重复的条件判断。
     * 实现逻辑：实现逻辑会逐项检查关键字段、状态或业务约束，一旦发现不满足条件的情况就立即抛出业务异常阻断流程。
     */
    private void validateQuery(AdminPaymentQueryDTO queryDTO) {
        if (queryDTO == null) {
            return;
        }
        if (queryDTO.getPayStatus() != null
                && !queryDTO.getPayStatus().isBlank()
                && !PayStatusEnum.isValid(queryDTO.getPayStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "payStatus 取值非法，只能为 UNPAID、PAID");
        }
        if (queryDTO.getRefundStatus() != null
                && !queryDTO.getRefundStatus().isBlank()
                && !RefundStatusEnum.isValid(queryDTO.getRefundStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "refundStatus 取值非法，只能为 NONE、REFUNDED");
        }
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 后台支付管理服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
    private AdminPaymentVO toAdminPaymentVO(Payment payment) {
        if (payment == null) {
            return null;
        }
        return AdminPaymentVO.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .payNo(payment.getPayNo())
                .payAmount(payment.getPayAmount())
                .payStatus(payment.getPayStatus())
                .payStatusDesc(PayStatusEnum.getDescByCode(payment.getPayStatus()))
                .refundAmount(payment.getRefundAmount())
                .refundStatus(payment.getRefundStatus())
                .refundStatusDesc(RefundStatusEnum.getDescByCode(payment.getRefundStatus()))
                .createdAt(payment.getCreatedAt())
                .build();
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 后台支付管理服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
    private AdminPaymentDetailVO toAdminPaymentDetailVO(Payment payment) {
        if (payment == null) {
            return null;
        }
        return AdminPaymentDetailVO.builder()
                .orderId(payment.getOrderId())
                .payNo(payment.getPayNo())
                .payAmount(payment.getPayAmount())
                .payStatus(payment.getPayStatus())
                .payStatusDesc(PayStatusEnum.getDescByCode(payment.getPayStatus()))
                .transactionId(payment.getTransactionId())
                .paidAt(payment.getPaidAt())
                .refundNo(payment.getRefundNo())
                .refundAmount(payment.getRefundAmount())
                .refundStatus(payment.getRefundStatus())
                .refundStatusDesc(RefundStatusEnum.getDescByCode(payment.getRefundStatus()))
                .refundAt(payment.getRefundAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
