package com.homechef.homechefsystem.controller.admin;

import com.homechef.homechefsystem.annotation.RequireAdmin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.AdminChefQueryDTO;
import com.homechef.homechefsystem.dto.AdminLoginDTO;
import com.homechef.homechefsystem.dto.AdminOrderQueryDTO;
import com.homechef.homechefsystem.dto.AdminStatusUpdateDTO;
import com.homechef.homechefsystem.dto.AdminUserQueryDTO;
import com.homechef.homechefsystem.dto.LoginTokenDTO;
import com.homechef.homechefsystem.service.AdminService;
import com.homechef.homechefsystem.utils.JwtUtil;
import com.homechef.homechefsystem.vo.AdminChefVO;
import com.homechef.homechefsystem.vo.AdminLoginVO;
import com.homechef.homechefsystem.vo.AdminOrderVO;
import com.homechef.homechefsystem.vo.AdminUserVO;
import com.homechef.homechefsystem.vo.OrderDetailVO;
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
public class AdminController {

    private final AdminService adminService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<LoginTokenDTO> login(@RequestBody AdminLoginDTO adminLoginDTO) {
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
    @GetMapping("/users")
    public Result<List<AdminUserVO>> getUserList(AdminUserQueryDTO queryDTO) {
        return Result.success(adminService.getUserList(queryDTO));
    }

    @RequireAdmin
    @PostMapping("/user/{id}/status")
    public Result<AdminUserVO> updateUserStatus(@PathVariable Long id,
                                                @RequestBody AdminStatusUpdateDTO statusUpdateDTO) {
        AdminUserVO adminUserVO = adminService.updateUserStatus(id, statusUpdateDTO);
        if (adminUserVO == null) {
            return Result.error(404, "user not found");
        }
        return Result.success(adminUserVO);
    }

    @RequireAdmin
    @GetMapping("/chefs")
    public Result<List<AdminChefVO>> getChefList(AdminChefQueryDTO queryDTO) {
        return Result.success(adminService.getChefList(queryDTO));
    }

    @RequireAdmin
    @PostMapping("/chef/{id}/status")
    public Result<AdminChefVO> updateChefStatus(@PathVariable Long id,
                                                @RequestBody AdminStatusUpdateDTO statusUpdateDTO) {
        AdminChefVO adminChefVO = adminService.updateChefStatus(id, statusUpdateDTO);
        if (adminChefVO == null) {
            return Result.error(404, "chef not found");
        }
        return Result.success(adminChefVO);
    }

    @RequireAdmin
    @GetMapping("/orders")
    public Result<List<AdminOrderVO>> getOrderList(AdminOrderQueryDTO queryDTO) {
        return Result.success(adminService.getOrderList(queryDTO));
    }

    @RequireAdmin
    @GetMapping("/order/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable Long id) {
        OrderDetailVO orderDetailVO = adminService.getOrderDetail(id);
        if (orderDetailVO == null) {
            return Result.error(404, "order not found");
        }
        return Result.success(orderDetailVO);
    }
}
