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

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 厨师档期服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
     */
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

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师档期服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public ChefScheduleVO getById(Long id) {
        return toChefScheduleVO(chefScheduleMapper.selectById(id));
    }

    /**
     * 方法说明：新增一条当前业务场景下的数据记录。
     * 主要作用：它承担 厨师档期服务实现 中的新增入口，把前端入参转换为可持久化的实体数据。
     * 实现逻辑：实现逻辑通常会先校验关键字段和归属关系，再组装实体写入数据库，最后返回新增后的最新结果。
     */
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

    /**
     * 方法说明：更新一条当前业务场景下的数据记录或状态。
     * 主要作用：该方法用于 厨师档期服务实现 中的编辑、状态变更或流程推进，保证外部只能修改业务允许变动的部分。
     * 实现逻辑：实现时会先查询原始数据并做归属或状态校验，再回填可编辑字段执行更新，必要时返回更新后的详情结果。
     */
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

    /**
     * 方法说明：更新一条当前业务场景下的数据记录或状态。
     * 主要作用：该方法用于 厨师档期服务实现 中的编辑、状态变更或流程推进，保证外部只能修改业务允许变动的部分。
     * 实现逻辑：实现时会先查询原始数据并做归属或状态校验，再回填可编辑字段执行更新，必要时返回更新后的详情结果。
     */
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

    /**
     * 方法说明：删除当前业务场景下指定的数据记录。
     * 主要作用：它为 厨师档期服务实现 提供清理数据的能力，同时确保删除动作仍然符合归属、状态或业务规则限制。
     * 实现逻辑：实现逻辑通常会先查询并校验目标记录，再执行删除；若前端需要回显删除前信息，则会在删除前先转换出返回对象。
     */
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

    /**
     * 方法说明：判断指定业务数据是否已经存在。
     * 主要作用：该方法用于 厨师档期服务实现 中的前置去重或存在性验证，避免重复创建或引用无效数据。
     * 实现逻辑：实现逻辑通常会根据主键、业务唯一键或关联条件调用 Mapper 统计结果，再把是否存在返回给上层流程使用。
     */
    @Override
    public boolean existsDuplicate(Long chefId, LocalDate serviceDate, String timeSlot, Long excludeId) {
        if (excludeId == null) {
            return chefScheduleMapper.countDuplicate(chefId, serviceDate, timeSlot) > 0;
        }
        return chefScheduleMapper.countDuplicateExcludeId(chefId, serviceDate, timeSlot, excludeId) > 0;
    }

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 厨师档期服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
     */
    @Override
    public List<ChefScheduleVO> getCurrentChefScheduleList(ChefScheduleQueryDTO queryDTO) {
        queryDTO.setChefId(requireCurrentChefId());
        return getScheduleList(queryDTO);
    }

    /**
     * 方法说明：新增一条当前业务场景下的数据记录。
     * 主要作用：它承担 厨师档期服务实现 中的新增入口，把前端入参转换为可持久化的实体数据。
     * 实现逻辑：实现逻辑通常会先校验关键字段和归属关系，再组装实体写入数据库，最后返回新增后的最新结果。
     */
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

    /**
     * 方法说明：更新一条当前业务场景下的数据记录或状态。
     * 主要作用：该方法用于 厨师档期服务实现 中的编辑、状态变更或流程推进，保证外部只能修改业务允许变动的部分。
     * 实现逻辑：实现时会先查询原始数据并做归属或状态校验，再回填可编辑字段执行更新，必要时返回更新后的详情结果。
     */
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

    /**
     * 方法说明：删除当前业务场景下指定的数据记录。
     * 主要作用：它为 厨师档期服务实现 提供清理数据的能力，同时确保删除动作仍然符合归属、状态或业务规则限制。
     * 实现逻辑：实现逻辑通常会先查询并校验目标记录，再执行删除；若前端需要回显删除前信息，则会在删除前先转换出返回对象。
     */
    @Override
    public ChefScheduleVO deleteCurrentChefSchedule(Long id) {
        ChefSchedule existingChefSchedule = getOwnedSchedule(id);
        if (existingChefSchedule.getLockedOrderId() != null) {
            throw new BusinessException(ResultCodeEnum.FAIL, "schedule is locked by order");
        }
        return deleteById(id);
    }

    /**
     * 方法说明：更新一条当前业务场景下的数据记录或状态。
     * 主要作用：该方法用于 厨师档期服务实现 中的编辑、状态变更或流程推进，保证外部只能修改业务允许变动的部分。
     * 实现逻辑：实现时会先查询原始数据并做归属或状态校验，再回填可编辑字段执行更新，必要时返回更新后的详情结果。
     */
    @Override
    public ChefScheduleVO updateCurrentChefScheduleAvailability(Long id, Integer isAvailable) {
        ChefSchedule existingChefSchedule = getOwnedSchedule(id);
        if (existingChefSchedule.getLockedOrderId() != null && Integer.valueOf(0).equals(isAvailable)) {
            throw new BusinessException(ResultCodeEnum.FAIL, "schedule is locked by order");
        }
        return updateAvailabilityById(id, isAvailable);
    }

    /**
     * 方法说明：在 厨师档期服务实现 中处理 disableExpiredAvailableSchedules 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
    public int disableExpiredAvailableSchedules() {
        LocalDate currentDate = LocalDate.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        return chefScheduleMapper.disableExpiredAvailableSchedules(currentDate, updatedAt);
    }

    /**
     * 方法说明：在 厨师档期服务实现 中处理 disableCurrentChefExpiredAvailableSchedules 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
    public int disableCurrentChefExpiredAvailableSchedules() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        return chefScheduleMapper.disableExpiredAvailableSchedulesByChefId(requireCurrentChefId(), currentTime, updatedAt);
    }

    /**
     * 方法说明：在 厨师档期服务实现 中处理 disableExpiredAvailableSchedulesByChefId 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
    public int disableExpiredAvailableSchedulesByChefId(Long chefId) {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        return chefScheduleMapper.disableExpiredAvailableSchedulesByChefId(chefId, currentTime, updatedAt);
    }

    /**
     * 方法说明：获取当前业务必需的数据，并在取不到时立即中断流程。
     * 主要作用：它把 厨师档期服务实现 中“查询 + 非空校验”的重复套路合并成一个辅助方法，让主流程更聚焦业务本身。
     * 实现逻辑：实现时会先根据身份信息或业务键查询目标数据，再补充坐标、状态或归属校验，不满足条件时直接抛出业务异常。
     */
    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师档期服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    private ChefSchedule getOwnedSchedule(Long id) {
        Long chefId = requireCurrentChefId();
        ChefSchedule chefSchedule = chefScheduleMapper.selectById(id);
        if (chefSchedule == null || !chefId.equals(chefSchedule.getChefId())) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "schedule not found");
        }
        return chefSchedule;
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 厨师档期服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
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

    /**
     * 方法说明：对输入值做统一的格式化和规范化处理。
     * 主要作用：该方法用于消除 厨师档期服务实现 中大小写、空白字符或别名写法带来的差异，保证后续逻辑按统一格式处理数据。
     * 实现逻辑：实现时会先做空值判断，再进行 trim、大小写转换或枚举标准化，最终返回可直接参与业务判断的值。
     */
    private String normalizeTimeSlot(String timeSlot) {
        TimeSlotEnum timeSlotEnum = TimeSlotEnum.fromCode(timeSlot);
        if (timeSlotEnum == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, TimeSlotEnum.INVALID_MESSAGE);
        }
        return timeSlotEnum.getCode();
    }
}
