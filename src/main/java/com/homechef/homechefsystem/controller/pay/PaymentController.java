package com.homechef.homechefsystem.controller.pay;

import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.PaymentCreateDTO;
import com.homechef.homechefsystem.dto.PaymentRefundDTO;
import com.homechef.homechefsystem.service.PaymentService;
import com.homechef.homechefsystem.vo.PaymentStatusVO;
import com.homechef.homechefsystem.vo.PaymentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
@Tag(name = "支付接口")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "创建支付单")
    @PostMapping("/create")
    public Result<PaymentVO> create(@RequestBody PaymentCreateDTO paymentCreateDTO) {
        if (!paymentService.orderExists(paymentCreateDTO.getOrderId())) {
            return Result.error(404, "order not found");
        }

        PaymentVO paymentVO = paymentService.create(paymentCreateDTO);
        if (paymentVO == null) {
            return Result.error(500, "create payment failed");
        }
        return Result.success(paymentVO);
    }

    @Operation(summary = "查询支付状态")
    @GetMapping("/status/{orderId}")
    public Result<PaymentStatusVO> getStatusByOrderId(@PathVariable Long orderId) {
        PaymentStatusVO paymentStatusVO = paymentService.getStatusByOrderId(orderId);
        if (paymentStatusVO == null) {
            return Result.error(404, "payment not found");
        }
        return Result.success(paymentStatusVO);
    }

    @Operation(summary = "模拟支付成功")
    @PostMapping("/mock-success/{orderId}")
    public Result<PaymentStatusVO> mockSuccessByOrderId(@PathVariable Long orderId) {
        if (!paymentService.orderExists(orderId)) {
            return Result.error(404, "order not found");
        }

        PaymentStatusVO existingPayment = paymentService.getStatusByOrderId(orderId);
        if (existingPayment == null) {
            return Result.error(404, "payment not found");
        }
        if ("PAID".equals(existingPayment.getPayStatus())) {
            return Result.error(400, "payment already paid");
        }

        PaymentStatusVO paymentStatusVO = paymentService.mockSuccessByOrderId(orderId);
        if (paymentStatusVO == null) {
            return Result.error(500, "mock pay failed");
        }
        return Result.success(paymentStatusVO);
    }

    @Operation(summary = "模拟退款")
    @PostMapping("/refund")
    public Result<PaymentStatusVO> refund(@RequestBody PaymentRefundDTO paymentRefundDTO) {
        if (!paymentService.orderExists(paymentRefundDTO.getOrderId())) {
            return Result.error(404, "order not found");
        }

        PaymentStatusVO existingPayment = paymentService.getStatusByOrderId(paymentRefundDTO.getOrderId());
        if (existingPayment == null) {
            return Result.error(404, "payment not found");
        }
        if (!"PAID".equals(existingPayment.getPayStatus())) {
            return Result.error(400, "payment status invalid");
        }
        if ("REFUNDED".equals(existingPayment.getRefundStatus())) {
            return Result.error(400, "payment already refunded");
        }

        PaymentStatusVO paymentStatusVO = paymentService.refund(paymentRefundDTO);
        if (paymentStatusVO == null) {
            return Result.error(500, "mock refund failed");
        }
        return Result.success(paymentStatusVO);
    }
}
