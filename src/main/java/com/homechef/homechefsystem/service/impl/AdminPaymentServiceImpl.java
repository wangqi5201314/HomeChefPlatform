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
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public AdminPaymentDetailVO getPaymentDetailByOrderId(Long orderId) {
        return toAdminPaymentDetailVO(paymentMapper.selectByOrderId(orderId));
    }

    /**
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
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
     * 检查当前传入的参数或业务状态是否合法。
     * 这个方法的作用，是把不合条件的情况尽早拦住，不让错误数据继续往下执行。
     * 它会根据规则逐项检查参数或状态，只要发现不满足条件，就直接抛出异常。
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
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
