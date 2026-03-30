package com.homechef.homechefsystem.controller.chef;

import com.homechef.homechefsystem.annotation.RequireLogin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.ChefServiceLocationSaveDTO;
import com.homechef.homechefsystem.service.ChefServiceLocationService;
import com.homechef.homechefsystem.vo.ChefServiceLocationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chef/service-location")
@RequiredArgsConstructor
@Tag(name = "厨师服务位置接口")
public class ChefServiceLocationController {

    private final ChefServiceLocationService chefServiceLocationService;

    @RequireLogin
    @Operation(summary = "查询当前登录厨师服务位置")
    @GetMapping("/me")
    public Result<ChefServiceLocationVO> getCurrentChefServiceLocation() {
        ChefServiceLocationVO chefServiceLocationVO = chefServiceLocationService.getCurrentChefServiceLocation();
        if (chefServiceLocationVO == null) {
            return Result.error(404, "service location not found");
        }
        return Result.success(chefServiceLocationVO);
    }

    @RequireLogin
    @Operation(summary = "保存或更新当前登录厨师服务位置")
    @PostMapping("/me")
    public Result<ChefServiceLocationVO> saveCurrentChefServiceLocation(@Valid @RequestBody ChefServiceLocationSaveDTO chefServiceLocationSaveDTO) {
        ChefServiceLocationVO chefServiceLocationVO = chefServiceLocationService.saveCurrentChefServiceLocation(chefServiceLocationSaveDTO);
        if (chefServiceLocationVO == null) {
            return Result.error(500, "save service location failed");
        }
        return Result.success(chefServiceLocationVO);
    }
}
