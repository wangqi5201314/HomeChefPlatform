package com.homechef.homechefsystem.controller.admin;

import com.homechef.homechefsystem.annotation.RequireAdmin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.AdminChefQueryDTO;
import com.homechef.homechefsystem.dto.AdminLoginDTO;
import com.homechef.homechefsystem.dto.LoginTokenDTO;
import com.homechef.homechefsystem.service.AdminService;
import com.homechef.homechefsystem.service.ChefScheduleService;
import com.homechef.homechefsystem.utils.JwtUtil;
import com.homechef.homechefsystem.vo.AdminChefVO;
import com.homechef.homechefsystem.vo.AdminLoginVO;
import com.homechef.homechefsystem.vo.OrderDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "后台管理接口")
public class AdminController {

    private final AdminService adminService;
    private final ChefScheduleService chefScheduleService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<LoginTokenDTO> login(@Valid @RequestBody AdminLoginDTO adminLoginDTO) {
        AdminLoginVO adminLoginVO = adminService.login(adminLoginDTO);
        if (adminLoginVO == null) {
            return Result.error(401, "username or password incorrect");
        }
        return Result.success(LoginTokenDTO.builder()
                .token(jwtUtil.generateAdminToken(adminLoginVO.getId()))
                .userType(JwtUtil.USER_TYPE_ADMIN)
                .adminId(adminLoginVO.getId())
                .build());
    }

    @RequireAdmin
    @Operation(summary = "查询厨师列表")
    @GetMapping("/chefs")
    public Result<List<AdminChefVO>> getChefList(AdminChefQueryDTO queryDTO) {
        return Result.success(adminService.getChefList(queryDTO));
    }

    @RequireAdmin
    @Operation(summary = "查询订单详情")
    @GetMapping("/order/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable Long id) {
        OrderDetailVO orderDetailVO = adminService.getOrderDetail(id);
        if (orderDetailVO == null) {
            return Result.error(404, "order not found");
        }
        return Result.success(orderDetailVO);
    }

    @RequireAdmin
    @Operation(summary = "手动触发禁用过期档期")
    @PostMapping("/chef/schedule/disable-expired")
    public Result<Integer> disableExpiredChefSchedules() {
        return Result.success(chefScheduleService.disableExpiredAvailableSchedules());
    }
}
