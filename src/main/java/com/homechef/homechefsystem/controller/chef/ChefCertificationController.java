package com.homechef.homechefsystem.controller.chef;

import com.homechef.homechefsystem.annotation.RequireLogin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.ChefCertificationSubmitDTO;
import com.homechef.homechefsystem.service.ChefCertificationService;
import com.homechef.homechefsystem.vo.ChefCertificationVO;
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
@RequestMapping("/api/chef/certification")
@RequiredArgsConstructor
@Tag(name = "厨师认证接口")
public class ChefCertificationController {

    private final ChefCertificationService chefCertificationService;

    @RequireLogin
    @Operation(summary = "查看我的认证资料")
    @GetMapping("/me")
    public Result<ChefCertificationVO> getCurrentChefCertification() {
        ChefCertificationVO chefCertificationVO = chefCertificationService.getCurrentChefCertification();
        if (chefCertificationVO == null) {
            return Result.error(404, "certification not found");
        }
        return Result.success(chefCertificationVO);
    }

    @RequireLogin
    @Operation(summary = "提交或更新我的认证资料")
    @PostMapping("/submit")
    public Result<ChefCertificationVO> submitCurrentChefCertification(@RequestBody ChefCertificationSubmitDTO chefCertificationSubmitDTO) {
        return Result.success(chefCertificationService.submitCurrentChefCertification(chefCertificationSubmitDTO));
    }

    @Operation(summary = "根据厨师ID查询认证详情")
    @GetMapping("/{chefId}")
    public Result<ChefCertificationVO> getByChefId(@PathVariable Long chefId) {
        ChefCertificationVO chefCertificationVO = chefCertificationService.getByChefId(chefId);
        if (chefCertificationVO == null) {
            return Result.error(404, "certification not found");
        }
        return Result.success(chefCertificationVO);
    }
}
