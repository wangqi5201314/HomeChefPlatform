package com.homechef.homechefsystem.controller.admin;

import com.homechef.homechefsystem.annotation.RequireAdmin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.service.ChefServiceLocationService;
import com.homechef.homechefsystem.vo.ChefServiceLocationVO;
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
@Tag(name = "后台厨师服务位置接口")
public class AdminChefServiceLocationController {

    private final ChefServiceLocationService chefServiceLocationService;

    @RequireAdmin
    @Operation(summary = "查询后台厨师服务位置列表")
    @GetMapping("/chef/{id}/service-locations")
    public Result<List<ChefServiceLocationVO>> getChefServiceLocationList(@PathVariable Long id) {
        return Result.success(chefServiceLocationService.getChefServiceLocationListByChefId(id));
    }
}
