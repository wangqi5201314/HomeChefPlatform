package com.homechef.homechefsystem.controller.chef;

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

    @Operation(summary = "查询档期详情")
    @GetMapping("/schedule/{id}")
    public Result<ChefScheduleVO> getById(@PathVariable Long id) {
        ChefScheduleVO chefScheduleVO = chefScheduleService.getById(id);
        if (chefScheduleVO == null) {
            return Result.error(404, "schedule not found");
        }
        return Result.success(chefScheduleVO);
    }

    @Operation(summary = "新增厨师档期")
    @PostMapping("/{chefId}/schedule")
    public Result<ChefScheduleVO> create(@PathVariable Long chefId,
                                         @RequestBody ChefScheduleCreateDTO chefScheduleCreateDTO) {
        if (chefScheduleService.existsDuplicate(chefId,
                chefScheduleCreateDTO.getServiceDate(),
                chefScheduleCreateDTO.getTimeSlot(),
                null)) {
            return Result.error(400, "schedule already exists");
        }

        ChefScheduleVO chefScheduleVO = chefScheduleService.create(chefId, chefScheduleCreateDTO);
        if (chefScheduleVO == null) {
            return Result.error(500, "create schedule failed");
        }
        return Result.success(chefScheduleVO);
    }

    @Operation(summary = "修改厨师档期")
    @PutMapping("/schedule/{id}")
    public Result<ChefScheduleVO> updateById(@PathVariable Long id,
                                             @RequestBody ChefScheduleUpdateDTO chefScheduleUpdateDTO) {
        ChefScheduleVO existingSchedule = chefScheduleService.getById(id);
        if (existingSchedule == null) {
            return Result.error(404, "schedule not found");
        }
        if (chefScheduleService.existsDuplicate(existingSchedule.getChefId(),
                chefScheduleUpdateDTO.getServiceDate(),
                chefScheduleUpdateDTO.getTimeSlot(),
                id)) {
            return Result.error(400, "schedule already exists");
        }
        if (existingSchedule.getLockedOrderId() != null && Integer.valueOf(0).equals(chefScheduleUpdateDTO.getIsAvailable())) {
            return Result.error(400, "schedule is locked by order");
        }

        ChefScheduleVO chefScheduleVO = chefScheduleService.updateById(id, chefScheduleUpdateDTO);
        if (chefScheduleVO == null) {
            return Result.error(404, "schedule not found");
        }
        return Result.success(chefScheduleVO);
    }

    @Operation(summary = "删除厨师档期")
    @DeleteMapping("/schedule/{id}")
    public Result<ChefScheduleVO> deleteById(@PathVariable Long id) {
        ChefScheduleVO existingSchedule = chefScheduleService.getById(id);
        if (existingSchedule == null) {
            return Result.error(404, "schedule not found");
        }
        if (existingSchedule.getLockedOrderId() != null) {
            return Result.error(400, "schedule is locked by order");
        }

        ChefScheduleVO chefScheduleVO = chefScheduleService.deleteById(id);
        if (chefScheduleVO == null) {
            return Result.error(404, "schedule not found");
        }
        return Result.success(chefScheduleVO);
    }

    @Operation(summary = "修改档期可预约状态")
    @PostMapping("/schedule/{id}/availability")
    public Result<ChefScheduleVO> updateAvailabilityById(@PathVariable Long id,
                                                         @RequestBody ChefScheduleAvailabilityDTO availabilityDTO) {
        ChefScheduleVO existingSchedule = chefScheduleService.getById(id);
        if (existingSchedule == null) {
            return Result.error(404, "schedule not found");
        }
        if (existingSchedule.getLockedOrderId() != null && Integer.valueOf(0).equals(availabilityDTO.getIsAvailable())) {
            return Result.error(400, "schedule is locked by order");
        }

        ChefScheduleVO chefScheduleVO = chefScheduleService.updateAvailabilityById(id, availabilityDTO.getIsAvailable());
        if (chefScheduleVO == null) {
            return Result.error(404, "schedule not found");
        }
        return Result.success(chefScheduleVO);
    }
}
