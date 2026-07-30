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

    /**
     * 新建一条业务数据。
     * 这个方法用于把前端提交的新信息正式写入数据库。
     * 它会先做必要的检查和组装，再保存数据，最后返回新建后的结果。
     */
    @Override
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

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public PaymentStatusVO getStatusByOrderId(Long orderId) {
        return toPaymentStatusVO(paymentMapper.selectByOrderId(orderId));
    }

    /**
     * 处理 mockSuccessByOrderId 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
    @Override
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

    /**
     * 处理 refund 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
    @Override
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

    /**
     * 判断某条数据是否已经存在。
     * 这个方法主要用于创建前去重，或者先判断关联数据是不是有效。
     * 它会按给定条件去查数据库，然后把是否存在的结果返回出来。
     */
    @Override
    public boolean orderExists(Long orderId) {
        return orderMapper.selectById(orderId) != null;
    }

    /**
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
     * 生成一个新的业务标识或编号。
     * 这个方法主要用来生成订单号、支付号、验证码这类不能手写的值。
     * 它会按固定规则把时间、随机数或前缀拼起来，最后返回一个可直接使用的新值。
     */
    private String generatePayNo() {
        return "PAY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    /**
     * 生成一个新的业务标识或编号。
     * 这个方法主要用来生成订单号、支付号、验证码这类不能手写的值。
     * 它会按固定规则把时间、随机数或前缀拼起来，最后返回一个可直接使用的新值。
     */
    private String generateTransactionId() {
        return "TXN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    /**
     * 生成一个新的业务标识或编号。
     * 这个方法主要用来生成订单号、支付号、验证码这类不能手写的值。
     * 它会按固定规则把时间、随机数或前缀拼起来，最后返回一个可直接使用的新值。
     */
    private String generateRefundNo() {
        return "REF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
