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
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
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
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public ChefScheduleVO getById(Long id) {
        return toChefScheduleVO(chefScheduleMapper.selectById(id));
    }

    /**
     * 新建一条业务数据。
     * 这个方法用于把前端提交的新信息正式写入数据库。
     * 它会先做必要的检查和组装，再保存数据，最后返回新建后的结果。
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
     * 修改一条已有的业务数据。
     * 这个方法用于更新详情信息、状态或某些可编辑字段。
     * 它会先查出原始数据，再把新值写进去，最后保存并返回更新后的结果。
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
     * 修改一条已有的业务数据。
     * 这个方法用于更新详情信息、状态或某些可编辑字段。
     * 它会先查出原始数据，再把新值写进去，最后保存并返回更新后的结果。
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
     * 删除一条不再需要的数据。
     * 这个方法主要用来清理记录，避免无效数据继续留在系统里。
     * 它通常会先查询要删的数据，确认没问题后再执行删除。
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
     * 判断某条数据是否已经存在。
     * 这个方法主要用于创建前去重，或者先判断关联数据是不是有效。
     * 它会按给定条件去查数据库，然后把是否存在的结果返回出来。
     */
    @Override
    public boolean existsDuplicate(Long chefId, LocalDate serviceDate, String timeSlot, Long excludeId) {
        if (excludeId == null) {
            return chefScheduleMapper.countDuplicate(chefId, serviceDate, timeSlot) > 0;
        }
        return chefScheduleMapper.countDuplicateExcludeId(chefId, serviceDate, timeSlot, excludeId) > 0;
    }

    /**
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
     */
    @Override
    public List<ChefScheduleVO> getCurrentChefScheduleList(ChefScheduleQueryDTO queryDTO) {
        queryDTO.setChefId(requireCurrentChefId());
        return getScheduleList(queryDTO);
    }

    /**
     * 新建一条业务数据。
     * 这个方法用于把前端提交的新信息正式写入数据库。
     * 它会先做必要的检查和组装，再保存数据，最后返回新建后的结果。
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
     * 修改一条已有的业务数据。
     * 这个方法用于更新详情信息、状态或某些可编辑字段。
     * 它会先查出原始数据，再把新值写进去，最后保存并返回更新后的结果。
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
     * 删除一条不再需要的数据。
     * 这个方法主要用来清理记录，避免无效数据继续留在系统里。
     * 它通常会先查询要删的数据，确认没问题后再执行删除。
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
     * 修改一条已有的业务数据。
     * 这个方法用于更新详情信息、状态或某些可编辑字段。
     * 它会先查出原始数据，再把新值写进去，最后保存并返回更新后的结果。
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
     * 处理 disableExpiredAvailableSchedules 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
    @Override
    public int disableExpiredAvailableSchedules() {
        LocalDate currentDate = LocalDate.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        return chefScheduleMapper.disableExpiredAvailableSchedules(currentDate, updatedAt);
    }

    /**
     * 处理 disableCurrentChefExpiredAvailableSchedules 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
    @Override
    public int disableCurrentChefExpiredAvailableSchedules() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        return chefScheduleMapper.disableExpiredAvailableSchedulesByChefId(requireCurrentChefId(), currentTime, updatedAt);
    }

    /**
     * 处理 disableExpiredAvailableSchedulesByChefId 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
    @Override
    public int disableExpiredAvailableSchedulesByChefId(Long chefId) {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        return chefScheduleMapper.disableExpiredAvailableSchedulesByChefId(chefId, currentTime, updatedAt);
    }

    /**
     * 查出当前业务必须要用的数据。
     * 这个方法用于把“先查数据，找不到就报错”这类逻辑集中到一起。
     * 它会根据 id 或当前登录信息去查记录，如果查不到或不符合条件，就直接抛出异常。
     */
    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
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
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
     * 把输入值整理成统一的格式。
     * 这个方法可以避免因为大小写、空格或不同写法导致后面的业务判断出错。
     * 它通常会先做 trim，再统一大小写，或者转成系统里约定好的标准值。
     */
    private String normalizeTimeSlot(String timeSlot) {
        TimeSlotEnum timeSlotEnum = TimeSlotEnum.fromCode(timeSlot);
        if (timeSlotEnum == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, TimeSlotEnum.INVALID_MESSAGE);
        }
        return timeSlotEnum.getCode();
    }
}
