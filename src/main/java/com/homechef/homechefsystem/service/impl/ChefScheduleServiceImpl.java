package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.dto.ChefScheduleCreateDTO;
import com.homechef.homechefsystem.dto.ChefScheduleQueryDTO;
import com.homechef.homechefsystem.dto.ChefScheduleUpdateDTO;
import com.homechef.homechefsystem.entity.ChefSchedule;
import com.homechef.homechefsystem.mapper.ChefScheduleMapper;
import com.homechef.homechefsystem.service.ChefScheduleService;
import com.homechef.homechefsystem.vo.ChefScheduleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChefScheduleServiceImpl implements ChefScheduleService {

    private final ChefScheduleMapper chefScheduleMapper;

    @Override
    public List<ChefScheduleVO> getScheduleList(ChefScheduleQueryDTO queryDTO) {
        List<ChefSchedule> chefScheduleList = chefScheduleMapper.selectList(queryDTO);
        if (chefScheduleList == null || chefScheduleList.isEmpty()) {
            return Collections.emptyList();
        }
        return chefScheduleList.stream()
                .map(this::toChefScheduleVO)
                .collect(Collectors.toList());
    }

    @Override
    public ChefScheduleVO getById(Long id) {
        return toChefScheduleVO(chefScheduleMapper.selectById(id));
    }

    @Override
    public ChefScheduleVO create(Long chefId, ChefScheduleCreateDTO chefScheduleCreateDTO) {
        LocalDateTime now = LocalDateTime.now();
        Integer isAvailable = chefScheduleCreateDTO.getIsAvailable();
        if (isAvailable == null) {
            isAvailable = 1;
        }

        ChefSchedule chefSchedule = ChefSchedule.builder()
                .chefId(chefId)
                .serviceDate(chefScheduleCreateDTO.getServiceDate())
                .timeSlot(chefScheduleCreateDTO.getTimeSlot())
                .startTime(chefScheduleCreateDTO.getStartTime())
                .endTime(chefScheduleCreateDTO.getEndTime())
                .isAvailable(isAvailable)
                .lockedOrderId(null)
                .remark(chefScheduleCreateDTO.getRemark())
                .createdAt(now)
                .updatedAt(now)
                .build();

        int rows = chefScheduleMapper.insert(chefSchedule);
        if (rows <= 0) {
            return null;
        }
        return toChefScheduleVO(chefScheduleMapper.selectById(chefSchedule.getId()));
    }

    @Override
    public ChefScheduleVO updateById(Long id, ChefScheduleUpdateDTO chefScheduleUpdateDTO) {
        ChefSchedule existingChefSchedule = chefScheduleMapper.selectById(id);
        if (existingChefSchedule == null) {
            return null;
        }

        Integer isAvailable = chefScheduleUpdateDTO.getIsAvailable();
        if (isAvailable == null) {
            isAvailable = 1;
        }

        existingChefSchedule.setServiceDate(chefScheduleUpdateDTO.getServiceDate());
        existingChefSchedule.setTimeSlot(chefScheduleUpdateDTO.getTimeSlot());
        existingChefSchedule.setStartTime(chefScheduleUpdateDTO.getStartTime());
        existingChefSchedule.setEndTime(chefScheduleUpdateDTO.getEndTime());
        existingChefSchedule.setIsAvailable(isAvailable);
        existingChefSchedule.setRemark(chefScheduleUpdateDTO.getRemark());
        existingChefSchedule.setUpdatedAt(LocalDateTime.now());

        int rows = chefScheduleMapper.updateById(existingChefSchedule);
        if (rows <= 0) {
            return null;
        }
        return toChefScheduleVO(chefScheduleMapper.selectById(id));
    }

    @Override
    public ChefScheduleVO updateAvailabilityById(Long id, Integer isAvailable) {
        ChefSchedule existingChefSchedule = chefScheduleMapper.selectById(id);
        if (existingChefSchedule == null) {
            return null;
        }

        int rows = chefScheduleMapper.updateAvailabilityById(id, isAvailable, LocalDateTime.now());
        if (rows <= 0) {
            return null;
        }
        return toChefScheduleVO(chefScheduleMapper.selectById(id));
    }

    @Override
    public ChefScheduleVO deleteById(Long id) {
        ChefSchedule existingChefSchedule = chefScheduleMapper.selectById(id);
        if (existingChefSchedule == null) {
            return null;
        }

        int rows = chefScheduleMapper.deleteById(id);
        if (rows <= 0) {
            return null;
        }
        return toChefScheduleVO(existingChefSchedule);
    }

    @Override
    public boolean existsDuplicate(Long chefId, LocalDate serviceDate, String timeSlot, Long excludeId) {
        if (excludeId == null) {
            return chefScheduleMapper.countDuplicate(chefId, serviceDate, timeSlot) > 0;
        }
        return chefScheduleMapper.countDuplicateExcludeId(chefId, serviceDate, timeSlot, excludeId) > 0;
    }

    private ChefScheduleVO toChefScheduleVO(ChefSchedule chefSchedule) {
        if (chefSchedule == null) {
            return null;
        }
        return ChefScheduleVO.builder()
                .id(chefSchedule.getId())
                .chefId(chefSchedule.getChefId())
                .serviceDate(chefSchedule.getServiceDate())
                .timeSlot(chefSchedule.getTimeSlot())
                .startTime(chefSchedule.getStartTime())
                .endTime(chefSchedule.getEndTime())
                .isAvailable(chefSchedule.getIsAvailable())
                .lockedOrderId(chefSchedule.getLockedOrderId())
                .remark(chefSchedule.getRemark())
                .build();
    }
}
