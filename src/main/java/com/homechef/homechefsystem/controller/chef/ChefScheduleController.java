package com.homechef.homechefsystem.controller.chef;

import com.homechef.homechefsystem.annotation.RequireLogin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.ChefScheduleAvailabilityDTO;
import com.homechef.homechefsystem.dto.ChefScheduleCreateDTO;
import com.homechef.homechefsystem.dto.ChefScheduleQueryDTO;
import com.homechef.homechefsystem.dto.ChefScheduleUpdateDTO;
import com.homechef.homechefsystem.service.ChefScheduleService;
import com.homechef.homechefsystem.vo.ChefScheduleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/chef")
@RequiredArgsConstructor
@Tag(name = "厨师档期接口")
public class ChefScheduleController {

    private final ChefScheduleService chefScheduleService;

    @Operation(summary = "查询厨师档期列表")
    @GetMapping("/{chefId}/schedule")
    public Result<List<ChefScheduleVO>> getScheduleList(@PathVariable Long chefId, ChefScheduleQueryDTO queryDTO) {
        queryDTO.setChefId(chefId);
        return Result.success(chefScheduleService.getScheduleList(queryDTO));
    }

    @RequireLogin
    @Operation(summary = "查询我的档期列表")
    @GetMapping("/schedule/my")
    public Result<List<ChefScheduleVO>> getCurrentChefScheduleList(ChefScheduleQueryDTO queryDTO) {
        return Result.success(chefScheduleService.getCurrentChefScheduleList(queryDTO));
    }

    @RequireLogin
    @Operation(summary = "新增我的档期")
    @PostMapping("/schedule")
    public Result<ChefScheduleVO> createCurrentChefSchedule(@RequestBody ChefScheduleCreateDTO chefScheduleCreateDTO) {
        ChefScheduleVO chefScheduleVO = chefScheduleService.createCurrentChefSchedule(chefScheduleCreateDTO);
        if (chefScheduleVO == null) {
            return Result.error(500, "create schedule failed");
        }
        return Result.success(chefScheduleVO);
    }

    @RequireLogin
    @Operation(summary = "修改我的档期")
    @PutMapping("/schedule/{id}")
    public Result<ChefScheduleVO> updateCurrentChefSchedule(@PathVariable Long id,
                                                            @RequestBody ChefScheduleUpdateDTO chefScheduleUpdateDTO) {
        ChefScheduleVO chefScheduleVO = chefScheduleService.updateCurrentChefSchedule(id, chefScheduleUpdateDTO);
        if (chefScheduleVO == null) {
            return Result.error(404, "schedule not found");
        }
        return Result.success(chefScheduleVO);
    }

    @RequireLogin
    @Operation(summary = "删除我的档期")
    @DeleteMapping("/schedule/{id}")
    public Result<ChefScheduleVO> deleteCurrentChefSchedule(@PathVariable Long id) {
        ChefScheduleVO chefScheduleVO = chefScheduleService.deleteCurrentChefSchedule(id);
        if (chefScheduleVO == null) {
            return Result.error(404, "schedule not found");
        }
        return Result.success(chefScheduleVO);
    }

    @RequireLogin
    @Operation(summary = "切换我的档期可预约状态")
    @PostMapping("/schedule/{id}/availability")
    public Result<ChefScheduleVO> updateCurrentChefScheduleAvailability(@PathVariable Long id,
                                                                        @RequestBody ChefScheduleAvailabilityDTO availabilityDTO) {
        ChefScheduleVO chefScheduleVO = chefScheduleService.updateCurrentChefScheduleAvailability(id, availabilityDTO.getIsAvailable());
        if (chefScheduleVO == null) {
            return Result.error(404, "schedule not found");
        }
        return Result.success(chefScheduleVO);
    }
}
