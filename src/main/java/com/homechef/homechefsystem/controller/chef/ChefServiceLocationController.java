package com.homechef.homechefsystem.controller.chef;

import com.homechef.homechefsystem.annotation.RequireLogin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.ChefServiceLocationCreateDTO;
import com.homechef.homechefsystem.dto.ChefServiceLocationUpdateDTO;
import com.homechef.homechefsystem.service.ChefServiceLocationService;
import com.homechef.homechefsystem.vo.ChefServiceLocationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chef/service-location")
@RequiredArgsConstructor
@Tag(name = "厨师服务位置接口")
public class ChefServiceLocationController {

    private final ChefServiceLocationService chefServiceLocationService;

    @RequireLogin
    @Operation(summary = "查询当前登录厨师服务位置列表")
    @GetMapping("/list")
    public Result<List<ChefServiceLocationVO>> getCurrentChefServiceLocationList() {
        return Result.success(chefServiceLocationService.getCurrentChefServiceLocationList());
    }

    @RequireLogin
    @Operation(summary = "查询当前登录厨师服务位置详情")
    @GetMapping("/{id}")
    public Result<ChefServiceLocationVO> getCurrentChefServiceLocationById(@PathVariable Long id) {
        ChefServiceLocationVO chefServiceLocationVO = chefServiceLocationService.getCurrentChefServiceLocationById(id);
        if (chefServiceLocationVO == null) {
            return Result.error(404, "service location not found");
        }
        return Result.success(chefServiceLocationVO);
    }

    @RequireLogin
    @Operation(summary = "新增服务位置")
    @PostMapping
    public Result<ChefServiceLocationVO> createCurrentChefServiceLocation(
            @Valid @RequestBody ChefServiceLocationCreateDTO chefServiceLocationCreateDTO) {
        ChefServiceLocationVO chefServiceLocationVO =
                chefServiceLocationService.createCurrentChefServiceLocation(chefServiceLocationCreateDTO);
        if (chefServiceLocationVO == null) {
            return Result.error(500, "create service location failed");
        }
        return Result.success(chefServiceLocationVO);
    }

    @RequireLogin
    @Operation(summary = "修改服务位置")
    @PutMapping("/{id}")
    public Result<ChefServiceLocationVO> updateCurrentChefServiceLocation(
            @PathVariable Long id,
            @Valid @RequestBody ChefServiceLocationUpdateDTO chefServiceLocationUpdateDTO) {
        ChefServiceLocationVO chefServiceLocationVO =
                chefServiceLocationService.updateCurrentChefServiceLocation(id, chefServiceLocationUpdateDTO);
        if (chefServiceLocationVO == null) {
            return Result.error(404, "service location not found");
        }
        return Result.success(chefServiceLocationVO);
    }

    @RequireLogin
    @Operation(summary = "删除服务位置")
    @DeleteMapping("/{id}")
    public Result<ChefServiceLocationVO> deleteCurrentChefServiceLocation(@PathVariable Long id) {
        ChefServiceLocationVO chefServiceLocationVO = chefServiceLocationService.deleteCurrentChefServiceLocation(id);
        if (chefServiceLocationVO == null) {
            return Result.error(404, "service location not found");
        }
        return Result.success(chefServiceLocationVO);
    }

    @RequireLogin
    @Operation(summary = "启用服务位置")
    @PostMapping("/{id}/activate")
    public Result<ChefServiceLocationVO> activateCurrentChefServiceLocation(@PathVariable Long id) {
        ChefServiceLocationVO chefServiceLocationVO = chefServiceLocationService.activateCurrentChefServiceLocation(id);
        if (chefServiceLocationVO == null) {
            return Result.error(404, "service location not found");
        }
        return Result.success(chefServiceLocationVO);
    }
}
