package com.homechef.homechefsystem.controller.chef;

import com.homechef.homechefsystem.annotation.RequireLogin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.ChefOrderRejectDTO;
import com.homechef.homechefsystem.service.ChefOrderService;
import com.homechef.homechefsystem.vo.ChefOrderDetailVO;
import com.homechef.homechefsystem.vo.ChefOrderListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chef/order")
@RequiredArgsConstructor
@RequireLogin
@Tag(name = "厨师订单接口")
public class ChefOrderController {

    private final ChefOrderService chefOrderService;

    @Operation(summary = "查询当前登录厨师的订单列表")
    @GetMapping("/list")
    public Result<List<ChefOrderListVO>> getCurrentChefOrderList(@RequestParam(required = false) String orderStatus) {
        return Result.success(chefOrderService.getCurrentChefOrderList(orderStatus));
    }

    @Operation(summary = "查询当前登录厨师的订单详情")
    @GetMapping("/{id}")
    public Result<ChefOrderDetailVO> getCurrentChefOrderDetail(@PathVariable Long id) {
        return Result.success(chefOrderService.getCurrentChefOrderDetail(id));
    }

    @Operation(summary = "接单")
    @PostMapping("/{id}/accept")
    public Result<ChefOrderDetailVO> accept(@PathVariable Long id) {
        return Result.success(chefOrderService.accept(id));
    }

    @Operation(summary = "拒单")
    @PostMapping("/{id}/reject")
    public Result<ChefOrderDetailVO> reject(@PathVariable Long id,
                                            @Valid @RequestBody ChefOrderRejectDTO chefOrderRejectDTO) {
        return Result.success(chefOrderService.reject(id, chefOrderRejectDTO));
    }

    @Operation(summary = "开始服务")
    @PostMapping("/{id}/start")
    public Result<ChefOrderDetailVO> start(@PathVariable Long id) {
        return Result.success(chefOrderService.start(id));
    }

    @Operation(summary = "完成服务")
    @PostMapping("/{id}/finish")
    public Result<ChefOrderDetailVO> finish(@PathVariable Long id) {
        return Result.success(chefOrderService.finish(id));
    }
}
