package com.homechef.homechefsystem.controller.admin;

import com.homechef.homechefsystem.annotation.RequireAdmin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.service.AdminDashboardService;
import com.homechef.homechefsystem.vo.AdminDashboardOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "后台首页总览接口")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @RequireAdmin
    @Operation(summary = "后台首页总览统计")
    @GetMapping("/overview")
    public Result<AdminDashboardOverviewVO> getOverview() {
        return Result.success(adminDashboardService.getOverview());
    }
}
