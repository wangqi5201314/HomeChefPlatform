package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.PaymentCreateDTO;
import com.homechef.homechefsystem.dto.PaymentRefundDTO;
import com.homechef.homechefsystem.vo.PaymentStatusVO;
import com.homechef.homechefsystem.vo.PaymentVO;

public interface PaymentService {

    PaymentVO create(PaymentCreateDTO paymentCreateDTO);

    PaymentStatusVO getStatusByOrderId(Long orderId);

    PaymentStatusVO mockSuccessByOrderId(Long orderId);

    PaymentStatusVO refund(PaymentRefundDTO paymentRefundDTO);

    boolean orderExists(Long orderId);
}
