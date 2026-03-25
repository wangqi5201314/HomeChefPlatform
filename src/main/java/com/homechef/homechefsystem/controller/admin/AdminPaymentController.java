package com.homechef.homechefsystem.controller.admin;

import com.homechef.homechefsystem.annotation.RequireAdmin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.AdminPaymentQueryDTO;
import com.homechef.homechefsystem.service.AdminPaymentService;
import com.homechef.homechefsystem.vo.AdminPaymentDetailVO;
import com.homechef.homechefsystem.vo.AdminPaymentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "后台支付管理接口")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @RequireAdmin
    @Operation(summary = "根据订单ID查询支付详情")
    @GetMapping("/payment/{orderId}")
    public Result<AdminPaymentDetailVO> getPaymentDetailByOrderId(@PathVariable Long orderId) {
        AdminPaymentDetailVO adminPaymentDetailVO = adminPaymentService.getPaymentDetailByOrderId(orderId);
        if (adminPaymentDetailVO == null) {
            return Result.error(404, "payment not found");
        }
        return Result.success(adminPaymentDetailVO);
    }

    @RequireAdmin
    @Operation(summary = "后台支付记录列表")
    @GetMapping("/payments")
    public Result<List<AdminPaymentVO>> getPaymentList(AdminPaymentQueryDTO queryDTO) {
        return Result.success(adminPaymentService.getPaymentList(queryDTO));
    }
}
