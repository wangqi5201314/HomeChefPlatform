package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.OrderStatusEnum;
import com.homechef.homechefsystem.common.enums.PayStatusEnum;
import com.homechef.homechefsystem.common.enums.RefundStatusEnum;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.PaymentCreateDTO;
import com.homechef.homechefsystem.dto.PaymentRefundDTO;
import com.homechef.homechefsystem.entity.Order;
import com.homechef.homechefsystem.entity.Payment;
import com.homechef.homechefsystem.mapper.OrderMapper;
import com.homechef.homechefsystem.mapper.PaymentMapper;
import com.homechef.homechefsystem.service.PaymentService;
import com.homechef.homechefsystem.vo.PaymentStatusVO;
import com.homechef.homechefsystem.vo.PaymentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String PAY_CHANNEL_WECHAT = "WECHAT";

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;

    @Override
    /**
     * 创建数据并返回处理结果。
     */
    public PaymentVO create(PaymentCreateDTO paymentCreateDTO) {
        Payment existingPayment = paymentMapper.selectByOrderId(paymentCreateDTO.getOrderId());
        if (existingPayment != null) {
            return toPaymentVO(existingPayment);
        }

        Order order = orderMapper.selectById(paymentCreateDTO.getOrderId());
        if (order == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .payNo(generatePayNo())
                .payChannel(PAY_CHANNEL_WECHAT)
                .payAmount(order.getPayAmount())
                .payStatus(PayStatusEnum.UNPAID.getCode())
                .transactionId(null)
                .paidAt(null)
                .refundNo(null)
                .refundAmount(null)
                .refundStatus(RefundStatusEnum.NONE.getCode())
                .refundAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        int rows = paymentMapper.insert(payment);
        if (rows <= 0) {
            return null;
        }
        return toPaymentVO(paymentMapper.selectByOrderId(order.getId()));
    }

    @Override
    /**
     * 查询指定订单对应的支付状态。
     */
    public PaymentStatusVO getStatusByOrderId(Long orderId) {
        return toPaymentStatusVO(paymentMapper.selectByOrderId(orderId));
    }

    @Override
    /**
     * 模拟指定订单支付成功。
     */
    public PaymentStatusVO mockSuccessByOrderId(Long orderId) {
        Payment payment = paymentMapper.selectByOrderId(orderId);
        if (payment == null) {
            return null;
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return null;
        }
        if (!OrderStatusEnum.WAIT_PAY.equalsCode(order.getOrderStatus())) {
            throw new BusinessException(ResultCodeEnum.FAIL, "仅待支付订单允许支付");
        }

        LocalDateTime now = LocalDateTime.now();
        int paymentRows = paymentMapper.updatePaySuccessByOrderId(
                orderId,
                PayStatusEnum.PAID.getCode(),
                generateTransactionId(),
                now,
                now
        );
        if (paymentRows <= 0) {
            return null;
        }

        orderMapper.updatePaidStatusById(orderId, OrderStatusEnum.PAID.getCode(), now);
        return toPaymentStatusVO(paymentMapper.selectByOrderId(orderId));
    }

    @Override
    /**
     * 处理 r ef un d 相关逻辑。
     */
    public PaymentStatusVO refund(PaymentRefundDTO paymentRefundDTO) {
        Payment payment = paymentMapper.selectByOrderId(paymentRefundDTO.getOrderId());
        if (payment == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        int paymentRows = paymentMapper.updateRefundByOrderId(
                paymentRefundDTO.getOrderId(),
                generateRefundNo(),
                paymentRefundDTO.getRefundAmount(),
                RefundStatusEnum.REFUNDED.getCode(),
                now,
                now
        );
        if (paymentRows <= 0) {
            return null;
        }

        orderMapper.updateRefundStatusById(
                paymentRefundDTO.getOrderId(),
                OrderStatusEnum.REFUNDED.getCode(),
                paymentRefundDTO.getRefundReason(),
                now
        );
        return toPaymentStatusVO(paymentMapper.selectByOrderId(paymentRefundDTO.getOrderId()));
    }

    @Override
    /**
     * 处理 o rd er ex is ts 相关逻辑。
     */
    public boolean orderExists(Long orderId) {
        return orderMapper.selectById(orderId) != null;
    }

    /**
     * 将实体对象转换为前端返回 VO。
     */
    private PaymentVO toPaymentVO(Payment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentVO.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .payNo(payment.getPayNo())
                .payChannel(payment.getPayChannel())
                .payAmount(payment.getPayAmount())
                .payStatus(payment.getPayStatus())
                .payStatusDesc(PayStatusEnum.getDescByCode(payment.getPayStatus()))
                .createdAt(payment.getCreatedAt())
                .build();
    }

    /**
     * 将实体对象转换为前端返回 VO。
     */
    private PaymentStatusVO toPaymentStatusVO(Payment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentStatusVO.builder()
                .orderId(payment.getOrderId())
                .payNo(payment.getPayNo())
                .payAmount(payment.getPayAmount())
                .payStatus(payment.getPayStatus())
                .payStatusDesc(PayStatusEnum.getDescByCode(payment.getPayStatus()))
                .transactionId(payment.getTransactionId())
                .paidAt(payment.getPaidAt())
                .refundAmount(payment.getRefundAmount())
                .refundStatus(payment.getRefundStatus())
                .refundStatusDesc(RefundStatusEnum.getDescByCode(payment.getRefundStatus()))
                .refundAt(payment.getRefundAt())
                .build();
    }

    /**
     * 生成支付单号。
     */
    private String generatePayNo() {
        return "PAY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    /**
     * 生成模拟交易流水号。
     */
    private String generateTransactionId() {
        return "TXN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    /**
     * 生成退款单号。
     */
    private String generateRefundNo() {
        return "REF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
