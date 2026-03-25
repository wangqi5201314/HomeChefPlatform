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

    @Override
    public AdminPaymentDetailVO getPaymentDetailByOrderId(Long orderId) {
        return toAdminPaymentDetailVO(paymentMapper.selectByOrderId(orderId));
    }

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
