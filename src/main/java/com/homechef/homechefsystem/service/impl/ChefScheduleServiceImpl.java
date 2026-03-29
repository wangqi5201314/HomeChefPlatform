package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.enums.TimeSlotEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.ChefScheduleCreateDTO;
import com.homechef.homechefsystem.dto.ChefScheduleQueryDTO;
import com.homechef.homechefsystem.dto.ChefScheduleUpdateDTO;
import com.homechef.homechefsystem.entity.ChefSchedule;
import com.homechef.homechefsystem.mapper.ChefScheduleMapper;
import com.homechef.homechefsystem.service.ChefScheduleService;
import com.homechef.homechefsystem.utils.LoginUserContext;
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
        String timeSlot = normalizeTimeSlot(chefScheduleCreateDTO.getTimeSlot());
        if (isAvailable == null) {
            isAvailable = 1;
        }

        ChefSchedule chefSchedule = ChefSchedule.builder()
                .chefId(chefId)
                .serviceDate(chefScheduleCreateDTO.getServiceDate())
                .timeSlot(timeSlot)
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
        String timeSlot = normalizeTimeSlot(chefScheduleUpdateDTO.getTimeSlot());
        if (isAvailable == null) {
            isAvailable = 1;
        }

        existingChefSchedule.setServiceDate(chefScheduleUpdateDTO.getServiceDate());
        existingChefSchedule.setTimeSlot(timeSlot);
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

    @Override
    public List<ChefScheduleVO> getCurrentChefScheduleList(ChefScheduleQueryDTO queryDTO) {
        queryDTO.setChefId(requireCurrentChefId());
        return getScheduleList(queryDTO);
    }

    @Override
    public ChefScheduleVO createCurrentChefSchedule(ChefScheduleCreateDTO chefScheduleCreateDTO) {
        Long chefId = requireCurrentChefId();
        String timeSlot = normalizeTimeSlot(chefScheduleCreateDTO.getTimeSlot());
        chefScheduleCreateDTO.setTimeSlot(timeSlot);
        if (existsDuplicate(chefId, chefScheduleCreateDTO.getServiceDate(), timeSlot, null)) {
            throw new BusinessException(ResultCodeEnum.FAIL, "schedule already exists");
        }
        return create(chefId, chefScheduleCreateDTO);
    }

    @Override
    public ChefScheduleVO updateCurrentChefSchedule(Long id, ChefScheduleUpdateDTO chefScheduleUpdateDTO) {
        ChefSchedule existingChefSchedule = getOwnedSchedule(id);
        String timeSlot = normalizeTimeSlot(chefScheduleUpdateDTO.getTimeSlot());
        chefScheduleUpdateDTO.setTimeSlot(timeSlot);
        if (existsDuplicate(existingChefSchedule.getChefId(),
                chefScheduleUpdateDTO.getServiceDate(),
                timeSlot,
                id)) {
            throw new BusinessException(ResultCodeEnum.FAIL, "schedule already exists");
        }
        if (existingChefSchedule.getLockedOrderId() != null && Integer.valueOf(0).equals(chefScheduleUpdateDTO.getIsAvailable())) {
            throw new BusinessException(ResultCodeEnum.FAIL, "schedule is locked by order");
        }
        return updateById(id, chefScheduleUpdateDTO);
    }

    @Override
    public ChefScheduleVO deleteCurrentChefSchedule(Long id) {
        ChefSchedule existingChefSchedule = getOwnedSchedule(id);
        if (existingChefSchedule.getLockedOrderId() != null) {
            throw new BusinessException(ResultCodeEnum.FAIL, "schedule is locked by order");
        }
        return deleteById(id);
    }

    @Override
    public ChefScheduleVO updateCurrentChefScheduleAvailability(Long id, Integer isAvailable) {
        ChefSchedule existingChefSchedule = getOwnedSchedule(id);
        if (existingChefSchedule.getLockedOrderId() != null && Integer.valueOf(0).equals(isAvailable)) {
            throw new BusinessException(ResultCodeEnum.FAIL, "schedule is locked by order");
        }
        return updateAvailabilityById(id, isAvailable);
    }

    @Override
    public int disableExpiredAvailableSchedules() {
        LocalDate currentDate = LocalDate.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        return chefScheduleMapper.disableExpiredAvailableSchedules(currentDate, updatedAt);
    }

    @Override
    public int disableCurrentChefExpiredAvailableSchedules() {
        LocalDate currentDate = LocalDate.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        return chefScheduleMapper.disableExpiredAvailableSchedulesByChefId(requireCurrentChefId(), currentDate, updatedAt);
    }

    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    private ChefSchedule getOwnedSchedule(Long id) {
        Long chefId = requireCurrentChefId();
        ChefSchedule chefSchedule = chefScheduleMapper.selectById(id);
        if (chefSchedule == null || !chefId.equals(chefSchedule.getChefId())) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "schedule not found");
        }
        return chefSchedule;
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
                .timeSlotDesc(TimeSlotEnum.getDescByCode(chefSchedule.getTimeSlot()))
                .startTime(chefSchedule.getStartTime())
                .endTime(chefSchedule.getEndTime())
                .isAvailable(chefSchedule.getIsAvailable())
                .lockedOrderId(chefSchedule.getLockedOrderId())
                .remark(chefSchedule.getRemark())
                .build();
    }

    private String normalizeTimeSlot(String timeSlot) {
        TimeSlotEnum timeSlotEnum = TimeSlotEnum.fromCode(timeSlot);
        if (timeSlotEnum == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, TimeSlotEnum.INVALID_MESSAGE);
        }
        return timeSlotEnum.getCode();
    }
}
