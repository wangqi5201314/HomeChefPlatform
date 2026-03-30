package com.homechef.homechefsystem.controller.admin;

import com.homechef.homechefsystem.annotation.RequireAdmin;
import com.homechef.homechefsystem.common.annotation.OperationLog;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.AdminStatusUpdateDTO;
import com.homechef.homechefsystem.service.AdminChefService;
import com.homechef.homechefsystem.vo.AdminChefDetailVO;
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

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "后台厨师管理接口")
public class AdminChefController {

    private final AdminChefService adminChefService;

    @RequireAdmin
    @OperationLog(module = "AdminChef", operation = "Update Chef Status")
    @Operation(summary = "修改厨师状态")
    @PostMapping("/chef/{id}/status")
    public Result<Void> updateChefStatus(@PathVariable Long id,
                                         @Valid @RequestBody AdminStatusUpdateDTO statusUpdateDTO) {
        adminChefService.updateChefStatus(id, statusUpdateDTO);
        return Result.success();
    }

    @RequireAdmin
    @Operation(summary = "查询后台厨师详情")
    @GetMapping("/chef/{id}")
    public Result<AdminChefDetailVO> getChefDetail(@PathVariable Long id) {
        AdminChefDetailVO adminChefDetailVO = adminChefService.getChefDetail(id);
        if (adminChefDetailVO == null) {
            return Result.error(404, "chef not found");
        }
        return Result.success(adminChefDetailVO);
    }
}
