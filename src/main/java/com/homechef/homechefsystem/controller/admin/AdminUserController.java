package com.homechef.homechefsystem.controller.admin;

import com.homechef.homechefsystem.annotation.RequireAdmin;
import com.homechef.homechefsystem.common.annotation.OperationLog;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.AdminStatusUpdateDTO;
import com.homechef.homechefsystem.dto.AdminUserQueryDTO;
import com.homechef.homechefsystem.service.AdminUserService;
import com.homechef.homechefsystem.vo.AdminUserDetailVO;
import com.homechef.homechefsystem.vo.AdminUserVO;
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
@Tag(name = "后台用户管理接口")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @RequireAdmin
    @Operation(summary = "查询用户列表")
    @GetMapping("/users")
    public Result<List<AdminUserVO>> getUserList(AdminUserQueryDTO queryDTO) {
        return Result.success(adminUserService.getUserList(queryDTO));
    }

    @RequireAdmin
    @Operation(summary = "查询用户详情")
    @GetMapping("/user/{id}")
    public Result<AdminUserDetailVO> getUserDetail(@PathVariable Long id) {
        AdminUserDetailVO adminUserDetailVO = adminUserService.getUserDetail(id);
        if (adminUserDetailVO == null) {
            return Result.error(404, "user not found");
        }
        return Result.success(adminUserDetailVO);
    }

    @RequireAdmin
    @OperationLog(module = "AdminUser", operation = "Update User Status")
    @Operation(summary = "修改用户状态")
    @PostMapping("/user/{id}/status")
    public Result<AdminUserVO> updateUserStatus(@PathVariable Long id,
                                                @Valid @RequestBody AdminStatusUpdateDTO statusUpdateDTO) {
        AdminUserVO adminUserVO = adminUserService.updateUserStatus(id, statusUpdateDTO);
        if (adminUserVO == null) {
            return Result.error(404, "user not found");
        }
        return Result.success(adminUserVO);
    }
}
