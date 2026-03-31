package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefScheduleCreateDTO;
import com.homechef.homechefsystem.dto.ChefScheduleQueryDTO;
import com.homechef.homechefsystem.dto.ChefScheduleUpdateDTO;
import com.homechef.homechefsystem.vo.ChefScheduleVO;

import java.time.LocalDate;
import java.util.List;

public interface ChefScheduleService {

    List<ChefScheduleVO> getScheduleList(ChefScheduleQueryDTO queryDTO);

    ChefScheduleVO getById(Long id);

    ChefScheduleVO create(Long chefId, ChefScheduleCreateDTO chefScheduleCreateDTO);

    ChefScheduleVO updateById(Long id, ChefScheduleUpdateDTO chefScheduleUpdateDTO);

    ChefScheduleVO updateAvailabilityById(Long id, Integer isAvailable);

    ChefScheduleVO deleteById(Long id);

    boolean existsDuplicate(Long chefId, LocalDate serviceDate, String timeSlot, Long excludeId);

    List<ChefScheduleVO> getCurrentChefScheduleList(ChefScheduleQueryDTO queryDTO);

    ChefScheduleVO createCurrentChefSchedule(ChefScheduleCreateDTO chefScheduleCreateDTO);

    ChefScheduleVO updateCurrentChefSchedule(Long id, ChefScheduleUpdateDTO chefScheduleUpdateDTO);

    ChefScheduleVO deleteCurrentChefSchedule(Long id);

    ChefScheduleVO updateCurrentChefScheduleAvailability(Long id, Integer isAvailable);

    int disableExpiredAvailableSchedules();

    int disableCurrentChefExpiredAvailableSchedules();

    int disableExpiredAvailableSchedulesByChefId(Long chefId);
}
