package com.homechef.homechefsystem.controller.admin;

import com.homechef.homechefsystem.annotation.RequireAdmin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.AdminOrderQueryDTO;
import com.homechef.homechefsystem.service.AdminOrderService;
import com.homechef.homechefsystem.vo.AdminOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "后台订单管理接口")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @RequireAdmin
    @Operation(summary = "查询订单列表")
    @GetMapping("/orders")
    public Result<List<AdminOrderVO>> getOrderList(AdminOrderQueryDTO queryDTO) {
        return Result.success(adminOrderService.getOrderList(queryDTO));
    }
}
