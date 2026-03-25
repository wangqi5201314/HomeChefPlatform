package com.homechef.homechefsystem.controller.admin;

import com.homechef.homechefsystem.annotation.RequireAdmin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.AdminReviewQueryDTO;
import com.homechef.homechefsystem.service.AdminReviewService;
import com.homechef.homechefsystem.vo.AdminReviewDetailVO;
import com.homechef.homechefsystem.vo.AdminReviewVO;
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
@Tag(name = "后台评价管理接口")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @RequireAdmin
    @Operation(summary = "查询评价列表")
    @GetMapping("/reviews")
    public Result<List<AdminReviewVO>> getReviewList(AdminReviewQueryDTO queryDTO) {
        return Result.success(adminReviewService.getReviewList(queryDTO));
    }

    @RequireAdmin
    @Operation(summary = "查询评价详情")
    @GetMapping("/review/{id}")
    public Result<AdminReviewDetailVO> getReviewDetail(@PathVariable Long id) {
        AdminReviewDetailVO adminReviewDetailVO = adminReviewService.getReviewDetail(id);
        if (adminReviewDetailVO == null) {
            return Result.error(404, "review not found");
        }
        return Result.success(adminReviewDetailVO);
    }
}
