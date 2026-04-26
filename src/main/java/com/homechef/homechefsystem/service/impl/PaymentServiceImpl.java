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
     * 方法说明：新增一条当前业务场景下的数据记录。
     * 主要作用：它承担 支付服务实现 中的新增入口，把前端入参转换为可持久化的实体数据。
     * 实现逻辑：实现逻辑通常会先校验关键字段和归属关系，再组装实体写入数据库，最后返回新增后的最新结果。
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
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 支付服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public PaymentStatusVO getStatusByOrderId(Long orderId) {
        return toPaymentStatusVO(paymentMapper.selectByOrderId(orderId));
    }

    /**
     * 方法说明：在 支付服务实现 中处理 mockSuccessByOrderId 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：在 支付服务实现 中处理 refund 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：判断指定业务数据是否已经存在。
     * 主要作用：该方法用于 支付服务实现 中的前置去重或存在性验证，避免重复创建或引用无效数据。
     * 实现逻辑：实现逻辑通常会根据主键、业务唯一键或关联条件调用 Mapper 统计结果，再把是否存在返回给上层流程使用。
     */
    @Override
    public boolean orderExists(Long orderId) {
        return orderMapper.selectById(orderId) != null;
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 支付服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 支付服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
     * 方法说明：生成当前业务流程所需的编号、验证码或标识值。
     * 主要作用：它为 支付服务实现 提供统一的标识生成能力，避免在主流程中混入随机数或格式拼接细节。
     * 实现逻辑：实现逻辑通常会结合时间、随机数或固定前缀构造结果，并确保生成值满足当前业务展示或唯一性需求。
     */
    private String generatePayNo() {
        return "PAY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    /**
     * 方法说明：生成当前业务流程所需的编号、验证码或标识值。
     * 主要作用：它为 支付服务实现 提供统一的标识生成能力，避免在主流程中混入随机数或格式拼接细节。
     * 实现逻辑：实现逻辑通常会结合时间、随机数或固定前缀构造结果，并确保生成值满足当前业务展示或唯一性需求。
     */
    private String generateTransactionId() {
        return "TXN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    /**
     * 方法说明：生成当前业务流程所需的编号、验证码或标识值。
     * 主要作用：它为 支付服务实现 提供统一的标识生成能力，避免在主流程中混入随机数或格式拼接细节。
     * 实现逻辑：实现逻辑通常会结合时间、随机数或固定前缀构造结果，并确保生成值满足当前业务展示或唯一性需求。
     */
    private String generateRefundNo() {
        return "REF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
