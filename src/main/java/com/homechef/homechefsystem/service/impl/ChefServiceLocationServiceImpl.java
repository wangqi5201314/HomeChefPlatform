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

    @Override
    /**
     * 获取当前登录厨师的服务位置列表。
     */
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

    @Override
    /**
     * 获取当前登录厨师名下的服务位置详情。
     */
    public ChefServiceLocationVO getCurrentChefServiceLocationById(Long id) {
        return toChefServiceLocationVO(getOwnedLocation(id));
    }

    @Override
    /**
     * 为当前登录厨师新增服务位置。
     */
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

    @Override
    /**
     * 更新当前登录厨师名下的服务位置。
     */
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

    @Override
    /**
     * 删除当前登录厨师名下的服务位置。
     */
    public ChefServiceLocationVO deleteCurrentChefServiceLocation(Long id) {
        ChefServiceLocation existingLocation = getOwnedLocation(id);
        int rows = chefServiceLocationMapper.deleteById(id, existingLocation.getChefId());
        if (rows <= 0) {
            return null;
        }
        return toChefServiceLocationVO(existingLocation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 启用当前登录厨师指定的服务位置。
     */
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

    @Override
    /**
     * 获取指定厨师的服务位置列表。
     */
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
     * 获取并校验当前登录厨师的 ID。
     */
    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    /**
     * 处理 r eq ui re ch ef ex is ts 相关逻辑。
     */
    private void requireChefExists(Long chefId) {
        Chef chef = chefMapper.selectById(chefId);
        if (chef == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "chef not found");
        }
    }

    /**
     * 处理 g et ow ne dl oc at io n 相关逻辑。
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
     * 校验输入参数或业务状态是否合法。
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
     * 处理 n or ma li ze te xt 相关逻辑。
     */
    private String normalizeText(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    /**
     * 将实体对象转换为前端返回 VO。
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
