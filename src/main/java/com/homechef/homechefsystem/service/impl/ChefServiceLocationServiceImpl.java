package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.ChefServiceLocationCreateDTO;
import com.homechef.homechefsystem.dto.ChefServiceLocationUpdateDTO;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.entity.ChefServiceLocation;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.ChefServiceLocationMapper;
import com.homechef.homechefsystem.service.ChefServiceLocationService;
import com.homechef.homechefsystem.utils.LoginUserContext;
import com.homechef.homechefsystem.vo.ChefServiceLocationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChefServiceLocationServiceImpl implements ChefServiceLocationService {

    private final ChefServiceLocationMapper chefServiceLocationMapper;
    private final ChefMapper chefMapper;

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 厨师服务位置管理实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
     */
    @Override
    public List<ChefServiceLocationVO> getCurrentChefServiceLocationList() {
        Long chefId = requireCurrentChefId();
        requireChefExists(chefId);
        List<ChefServiceLocation> chefServiceLocationList = chefServiceLocationMapper.selectListByChefId(chefId);
        if (chefServiceLocationList == null || chefServiceLocationList.isEmpty()) {
            return Collections.emptyList();
        }
        return chefServiceLocationList.stream()
                .map(this::toChefServiceLocationVO)
                .collect(Collectors.toList());
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师服务位置管理实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public ChefServiceLocationVO getCurrentChefServiceLocationById(Long id) {
        return toChefServiceLocationVO(getOwnedLocation(id));
    }

    /**
     * 方法说明：新增一条当前业务场景下的数据记录。
     * 主要作用：它承担 厨师服务位置管理实现 中的新增入口，把前端入参转换为可持久化的实体数据。
     * 实现逻辑：实现逻辑通常会先校验关键字段和归属关系，再组装实体写入数据库，最后返回新增后的最新结果。
     */
    @Override
    public ChefServiceLocationVO createCurrentChefServiceLocation(ChefServiceLocationCreateDTO chefServiceLocationCreateDTO) {
        Long chefId = requireCurrentChefId();
        requireChefExists(chefId);
        validateLocationFields(
                chefServiceLocationCreateDTO.getProvince(),
                chefServiceLocationCreateDTO.getCity(),
                chefServiceLocationCreateDTO.getDistrict(),
                chefServiceLocationCreateDTO.getDetailAddress(),
                chefServiceLocationCreateDTO.getLongitude(),
                chefServiceLocationCreateDTO.getLatitude()
        );

        LocalDateTime now = LocalDateTime.now();
        ChefServiceLocation chefServiceLocation = ChefServiceLocation.builder()
                .chefId(chefId)
                .locationName(normalizeText(chefServiceLocationCreateDTO.getLocationName()))
                .province(normalizeText(chefServiceLocationCreateDTO.getProvince()))
                .city(normalizeText(chefServiceLocationCreateDTO.getCity()))
                .district(normalizeText(chefServiceLocationCreateDTO.getDistrict()))
                .town(normalizeText(chefServiceLocationCreateDTO.getTown()))
                .detailAddress(normalizeText(chefServiceLocationCreateDTO.getDetailAddress()))
                .longitude(chefServiceLocationCreateDTO.getLongitude())
                .latitude(chefServiceLocationCreateDTO.getLatitude())
                .isActive(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        int rows = chefServiceLocationMapper.insert(chefServiceLocation);
        if (rows <= 0) {
            return null;
        }
        return toChefServiceLocationVO(chefServiceLocationMapper.selectById(chefServiceLocation.getId()));
    }

    /**
     * 方法说明：更新一条当前业务场景下的数据记录或状态。
     * 主要作用：该方法用于 厨师服务位置管理实现 中的编辑、状态变更或流程推进，保证外部只能修改业务允许变动的部分。
     * 实现逻辑：实现时会先查询原始数据并做归属或状态校验，再回填可编辑字段执行更新，必要时返回更新后的详情结果。
     */
    @Override
    public ChefServiceLocationVO updateCurrentChefServiceLocation(Long id, ChefServiceLocationUpdateDTO chefServiceLocationUpdateDTO) {
        validateLocationFields(
                chefServiceLocationUpdateDTO.getProvince(),
                chefServiceLocationUpdateDTO.getCity(),
                chefServiceLocationUpdateDTO.getDistrict(),
                chefServiceLocationUpdateDTO.getDetailAddress(),
                chefServiceLocationUpdateDTO.getLongitude(),
                chefServiceLocationUpdateDTO.getLatitude()
        );

        ChefServiceLocation existingLocation = getOwnedLocation(id);
        existingLocation.setLocationName(normalizeText(chefServiceLocationUpdateDTO.getLocationName()));
        existingLocation.setProvince(normalizeText(chefServiceLocationUpdateDTO.getProvince()));
        existingLocation.setCity(normalizeText(chefServiceLocationUpdateDTO.getCity()));
        existingLocation.setDistrict(normalizeText(chefServiceLocationUpdateDTO.getDistrict()));
        existingLocation.setTown(normalizeText(chefServiceLocationUpdateDTO.getTown()));
        existingLocation.setDetailAddress(normalizeText(chefServiceLocationUpdateDTO.getDetailAddress()));
        existingLocation.setLongitude(chefServiceLocationUpdateDTO.getLongitude());
        existingLocation.setLatitude(chefServiceLocationUpdateDTO.getLatitude());
        existingLocation.setUpdatedAt(LocalDateTime.now());

        int rows = chefServiceLocationMapper.updateById(existingLocation);
        if (rows <= 0) {
            return null;
        }
        return toChefServiceLocationVO(chefServiceLocationMapper.selectById(id));
    }

    /**
     * 方法说明：删除当前业务场景下指定的数据记录。
     * 主要作用：它为 厨师服务位置管理实现 提供清理数据的能力，同时确保删除动作仍然符合归属、状态或业务规则限制。
     * 实现逻辑：实现逻辑通常会先查询并校验目标记录，再执行删除；若前端需要回显删除前信息，则会在删除前先转换出返回对象。
     */
    @Override
    public ChefServiceLocationVO deleteCurrentChefServiceLocation(Long id) {
        ChefServiceLocation existingLocation = getOwnedLocation(id);
        int rows = chefServiceLocationMapper.deleteById(id, existingLocation.getChefId());
        if (rows <= 0) {
            return null;
        }
        return toChefServiceLocationVO(existingLocation);
    }

    /**
     * 方法说明：在 厨师服务位置管理实现 中处理 activateCurrentChefServiceLocation 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChefServiceLocationVO activateCurrentChefServiceLocation(Long id) {
        ChefServiceLocation existingLocation = getOwnedLocation(id);
        LocalDateTime now = LocalDateTime.now();
        chefServiceLocationMapper.resetActiveByChefId(existingLocation.getChefId(), now);
        int rows = chefServiceLocationMapper.activateById(id, existingLocation.getChefId(), now);
        if (rows <= 0) {
            return null;
        }
        return toChefServiceLocationVO(chefServiceLocationMapper.selectById(id));
    }

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 厨师服务位置管理实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
     */
    @Override
    public List<ChefServiceLocationVO> getChefServiceLocationListByChefId(Long chefId) {
        requireChefExists(chefId);
        List<ChefServiceLocation> chefServiceLocationList = chefServiceLocationMapper.selectListByChefId(chefId);
        if (chefServiceLocationList == null || chefServiceLocationList.isEmpty()) {
            return Collections.emptyList();
        }
        return chefServiceLocationList.stream()
                .map(this::toChefServiceLocationVO)
                .collect(Collectors.toList());
    }

    /**
     * 方法说明：获取当前业务必需的数据，并在取不到时立即中断流程。
     * 主要作用：它把 厨师服务位置管理实现 中“查询 + 非空校验”的重复套路合并成一个辅助方法，让主流程更聚焦业务本身。
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
     * 方法说明：获取当前业务必需的数据，并在取不到时立即中断流程。
     * 主要作用：它把 厨师服务位置管理实现 中“查询 + 非空校验”的重复套路合并成一个辅助方法，让主流程更聚焦业务本身。
     * 实现逻辑：实现时会先根据身份信息或业务键查询目标数据，再补充坐标、状态或归属校验，不满足条件时直接抛出业务异常。
     */
    private void requireChefExists(Long chefId) {
        Chef chef = chefMapper.selectById(chefId);
        if (chef == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "chef not found");
        }
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师服务位置管理实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    private ChefServiceLocation getOwnedLocation(Long id) {
        Long chefId = requireCurrentChefId();
        requireChefExists(chefId);
        ChefServiceLocation chefServiceLocation = chefServiceLocationMapper.selectByChefIdAndId(chefId, id);
        if (chefServiceLocation == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "service location not found");
        }
        return chefServiceLocation;
    }

    /**
     * 方法说明：校验当前业务输入或状态是否满足执行条件。
     * 主要作用：它用于把 厨师服务位置管理实现 中的前置规则集中收口，避免核心流程夹杂过多重复的条件判断。
     * 实现逻辑：实现逻辑会逐项检查关键字段、状态或业务约束，一旦发现不满足条件的情况就立即抛出业务异常阻断流程。
     */
    private void validateLocationFields(String province,
                                        String city,
                                        String district,
                                        String detailAddress,
                                        BigDecimal longitude,
                                        BigDecimal latitude) {
        if (!StringUtils.hasText(province)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "province 不能为空");
        }
        if (!StringUtils.hasText(city)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "city 不能为空");
        }
        if (!StringUtils.hasText(district)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "district 不能为空");
        }
        if (!StringUtils.hasText(detailAddress)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "detailAddress 不能为空");
        }
        if (longitude == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "longitude 不能为空");
        }
        if (latitude == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "latitude 不能为空");
        }
    }

    /**
     * 方法说明：对输入值做统一的格式化和规范化处理。
     * 主要作用：该方法用于消除 厨师服务位置管理实现 中大小写、空白字符或别名写法带来的差异，保证后续逻辑按统一格式处理数据。
     * 实现逻辑：实现时会先做空值判断，再进行 trim、大小写转换或枚举标准化，最终返回可直接参与业务判断的值。
     */
    private String normalizeText(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 厨师服务位置管理实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
    private ChefServiceLocationVO toChefServiceLocationVO(ChefServiceLocation chefServiceLocation) {
        if (chefServiceLocation == null) {
            return null;
        }
        return ChefServiceLocationVO.builder()
                .id(chefServiceLocation.getId())
                .chefId(chefServiceLocation.getChefId())
                .locationName(chefServiceLocation.getLocationName())
                .province(chefServiceLocation.getProvince())
                .city(chefServiceLocation.getCity())
                .district(chefServiceLocation.getDistrict())
                .town(chefServiceLocation.getTown())
                .detailAddress(chefServiceLocation.getDetailAddress())
                .longitude(chefServiceLocation.getLongitude())
                .latitude(chefServiceLocation.getLatitude())
                .isActive(chefServiceLocation.getIsActive())
                .createdAt(chefServiceLocation.getCreatedAt())
                .updatedAt(chefServiceLocation.getUpdatedAt())
                .build();
    }
}
