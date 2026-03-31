package com.homechef.homechefsystem.controller.chef;

import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.ChefQueryDTO;
import com.homechef.homechefsystem.dto.ChefRecommendQueryDTO;
import com.homechef.homechefsystem.dto.ChefUpdateDTO;
import com.homechef.homechefsystem.service.ChefRecommendService;
import com.homechef.homechefsystem.service.ChefService;
import com.homechef.homechefsystem.vo.ChefDetailVO;
import com.homechef.homechefsystem.vo.ChefListVO;
import com.homechef.homechefsystem.vo.ChefRecommendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chef")
@RequiredArgsConstructor
@Tag(name = "厨师接口")
public class ChefController {

    private final ChefService chefService;
    private final ChefRecommendService chefRecommendService;

    @Operation(summary = "查询厨师列表")
    @GetMapping("/list")
    public Result<List<ChefListVO>> getChefList(ChefQueryDTO queryDTO) {
        return Result.success(chefService.getChefList(queryDTO));
    }

    @Operation(summary = "根据ID查询厨师详情")
    @GetMapping("/{id}")
    public Result<ChefDetailVO> getById(@PathVariable Long id) {
        ChefDetailVO chefDetailVO = chefService.getById(id);
        if (chefDetailVO == null) {
            return Result.error(404, "chef not found");
        }
        return Result.success(chefDetailVO);
    }

    @Operation(summary = "首页厨师推荐")
    @PostMapping("/recommend")
    public Result<List<ChefRecommendVO>> recommend(@Valid @RequestBody ChefRecommendQueryDTO chefRecommendQueryDTO) {
        return Result.success(chefRecommendService.recommend(chefRecommendQueryDTO));
    }

    @Operation(summary = "修改厨师信息")
    @PutMapping("/{id}")
    public Result<ChefDetailVO> updateById(@PathVariable Long id, @RequestBody ChefUpdateDTO chefUpdateDTO) {
        ChefDetailVO chefDetailVO = chefService.updateById(id, chefUpdateDTO);
        if (chefDetailVO == null) {
            return Result.error(404, "chef not found");
        }
        return Result.success(chefDetailVO);
    }
}
