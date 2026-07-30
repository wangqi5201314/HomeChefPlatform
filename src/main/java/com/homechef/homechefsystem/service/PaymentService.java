package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.PaymentCreateDTO;
import com.homechef.homechefsystem.dto.PaymentRefundDTO;
import com.homechef.homechefsystem.vo.PaymentStatusVO;
import com.homechef.homechefsystem.vo.PaymentVO;

public interface PaymentService {

    /**
     * 创建数据并返回处理结果。
     */
    PaymentVO create(PaymentCreateDTO paymentCreateDTO);

    /**
     * 查询指定订单的支付状态。
     */
    PaymentStatusVO getStatusByOrderId(Long orderId);

    /**
     * 模拟指定订单支付成功后的状态。
     */
    PaymentStatusVO mockSuccessByOrderId(Long orderId);

    /**
     * 处理 r ef un d 相关业务。
     */
    PaymentStatusVO refund(PaymentRefundDTO paymentRefundDTO);

    /**
     * 判断指定订单是否存在。
     */
    boolean orderExists(Long orderId);
}
