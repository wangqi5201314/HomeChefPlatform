package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefScheduleCreateDTO;
import com.homechef.homechefsystem.dto.ChefScheduleQueryDTO;
import com.homechef.homechefsystem.dto.ChefScheduleUpdateDTO;
import com.homechef.homechefsystem.vo.ChefScheduleVO;

import java.time.LocalDate;
import java.util.List;

public interface ChefScheduleService {

    /**
     * 查询列表数据并返回结果。
     */
    List<ChefScheduleVO> getScheduleList(ChefScheduleQueryDTO queryDTO);

    /**
     * 根据 ID 查询对应数据。
     */
    ChefScheduleVO getById(Long id);

    /**
     * 创建数据并返回处理结果。
     */
    ChefScheduleVO create(Long chefId, ChefScheduleCreateDTO chefScheduleCreateDTO);

    /**
     * 根据 ID 更新数据并返回结果。
     */
    ChefScheduleVO updateById(Long id, ChefScheduleUpdateDTO chefScheduleUpdateDTO);

    /**
     * 根据 ID 更新指定数据并返回结果。
     */
    ChefScheduleVO updateAvailabilityById(Long id, Integer isAvailable);

    /**
     * 根据 ID 删除指定数据并返回结果。
     */
    ChefScheduleVO deleteById(Long id);

    /**
     * 处理 e xi st sd up li ca te 相关业务。
     */
    boolean existsDuplicate(Long chefId, LocalDate serviceDate, String timeSlot, Long excludeId);

    /**
     * 获取当前登录主体的资料信息。
     */
    List<ChefScheduleVO> getCurrentChefScheduleList(ChefScheduleQueryDTO queryDTO);

    /**
     * 处理 c re at ec ur re nt ch ef sc he du le 相关业务。
     */
    ChefScheduleVO createCurrentChefSchedule(ChefScheduleCreateDTO chefScheduleCreateDTO);

    /**
     * 更新当前登录主体的资料信息。
     */
    ChefScheduleVO updateCurrentChefSchedule(Long id, ChefScheduleUpdateDTO chefScheduleUpdateDTO);

    /**
     * 处理 d el et ec ur re nt ch ef sc he du le 相关业务。
     */
    ChefScheduleVO deleteCurrentChefSchedule(Long id);

    /**
     * 更新当前登录主体的资料信息。
     */
    ChefScheduleVO updateCurrentChefScheduleAvailability(Long id, Integer isAvailable);

    /**
     * 批量禁用所有已过期的可预约档期。
     */
    int disableExpiredAvailableSchedules();

    /**
     * 禁用当前登录厨师已过期的可预约档期。
     */
    int disableCurrentChefExpiredAvailableSchedules();

    /**
     * 禁用指定厨师已过期的可预约档期。
     */
    int disableExpiredAvailableSchedulesByChefId(Long chefId);
}
