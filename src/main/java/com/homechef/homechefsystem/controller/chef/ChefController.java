package com.homechef.homechefsystem.controller.chef;

import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.ChefQueryDTO;
import com.homechef.homechefsystem.dto.ChefUpdateDTO;
import com.homechef.homechefsystem.service.ChefService;
import com.homechef.homechefsystem.vo.ChefDetailVO;
import com.homechef.homechefsystem.vo.ChefListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chef")
@RequiredArgsConstructor
public class ChefController {

    private final ChefService chefService;

    @GetMapping("/list")
    public Result<List<ChefListVO>> getChefList(ChefQueryDTO queryDTO) {
        return Result.success(chefService.getChefList(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<ChefDetailVO> getById(@PathVariable Long id) {
        ChefDetailVO chefDetailVO = chefService.getById(id);
        if (chefDetailVO == null) {
            return Result.error(404, "chef not found");
        }
        return Result.success(chefDetailVO);
    }

    @PutMapping("/{id}")
    public Result<ChefDetailVO> updateById(@PathVariable Long id, @RequestBody ChefUpdateDTO chefUpdateDTO) {
        ChefDetailVO chefDetailVO = chefService.updateById(id, chefUpdateDTO);
        if (chefDetailVO == null) {
            return Result.error(404, "chef not found");
        }
        return Result.success(chefDetailVO);
    }
}
