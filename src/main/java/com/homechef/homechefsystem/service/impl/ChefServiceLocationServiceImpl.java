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
    public ChefServiceLocationVO getCurrentChefServiceLocationById(Long id) {
        return toChefServiceLocationVO(getOwnedLocation(id));
    }

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

    @Override
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

    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    private void requireChefExists(Long chefId) {
        Chef chef = chefMapper.selectById(chefId);
        if (chef == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "chef not found");
        }
    }

    private ChefServiceLocation getOwnedLocation(Long id) {
        Long chefId = requireCurrentChefId();
        requireChefExists(chefId);
        ChefServiceLocation chefServiceLocation = chefServiceLocationMapper.selectByChefIdAndId(chefId, id);
        if (chefServiceLocation == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "service location not found");
        }
        return chefServiceLocation;
    }

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

    private String normalizeText(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

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
