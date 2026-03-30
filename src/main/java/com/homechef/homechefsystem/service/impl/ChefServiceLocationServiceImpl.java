package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.ChefServiceLocationSaveDTO;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.entity.ChefServiceLocation;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.ChefServiceLocationMapper;
import com.homechef.homechefsystem.service.ChefServiceLocationService;
import com.homechef.homechefsystem.utils.LoginUserContext;
import com.homechef.homechefsystem.vo.ChefServiceLocationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChefServiceLocationServiceImpl implements ChefServiceLocationService {

    private final ChefServiceLocationMapper chefServiceLocationMapper;
    private final ChefMapper chefMapper;

    @Override
    public ChefServiceLocationVO getCurrentChefServiceLocation() {
        Long chefId = requireCurrentChefId();
        requireChefExists(chefId);
        return toChefServiceLocationVO(chefServiceLocationMapper.selectByChefId(chefId));
    }

    @Override
    public ChefServiceLocationVO saveCurrentChefServiceLocation(ChefServiceLocationSaveDTO chefServiceLocationSaveDTO) {
        Long chefId = requireCurrentChefId();
        requireChefExists(chefId);
        validateSaveDTO(chefServiceLocationSaveDTO);

        LocalDateTime now = LocalDateTime.now();
        ChefServiceLocation existingLocation = chefServiceLocationMapper.selectByChefId(chefId);
        if (existingLocation == null) {
            ChefServiceLocation chefServiceLocation = ChefServiceLocation.builder()
                    .chefId(chefId)
                    .province(chefServiceLocationSaveDTO.getProvince())
                    .city(chefServiceLocationSaveDTO.getCity())
                    .district(chefServiceLocationSaveDTO.getDistrict())
                    .town(chefServiceLocationSaveDTO.getTown())
                    .detailAddress(chefServiceLocationSaveDTO.getDetailAddress())
                    .longitude(chefServiceLocationSaveDTO.getLongitude())
                    .latitude(chefServiceLocationSaveDTO.getLatitude())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            int rows = chefServiceLocationMapper.insert(chefServiceLocation);
            if (rows <= 0) {
                return null;
            }
        } else {
            existingLocation.setProvince(chefServiceLocationSaveDTO.getProvince());
            existingLocation.setCity(chefServiceLocationSaveDTO.getCity());
            existingLocation.setDistrict(chefServiceLocationSaveDTO.getDistrict());
            existingLocation.setTown(chefServiceLocationSaveDTO.getTown());
            existingLocation.setDetailAddress(chefServiceLocationSaveDTO.getDetailAddress());
            existingLocation.setLongitude(chefServiceLocationSaveDTO.getLongitude());
            existingLocation.setLatitude(chefServiceLocationSaveDTO.getLatitude());
            existingLocation.setUpdatedAt(now);
            int rows = chefServiceLocationMapper.updateByChefId(existingLocation);
            if (rows <= 0) {
                return null;
            }
        }
        return toChefServiceLocationVO(chefServiceLocationMapper.selectByChefId(chefId));
    }

    @Override
    public ChefServiceLocationVO getByChefId(Long chefId) {
        requireChefExists(chefId);
        return toChefServiceLocationVO(chefServiceLocationMapper.selectByChefId(chefId));
    }

    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    private Chef requireChefExists(Long chefId) {
        Chef chef = chefMapper.selectById(chefId);
        if (chef == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "chef not found");
        }
        return chef;
    }

    private void validateSaveDTO(ChefServiceLocationSaveDTO chefServiceLocationSaveDTO) {
        if (!StringUtils.hasText(chefServiceLocationSaveDTO.getProvince())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "province 不能为空");
        }
        if (!StringUtils.hasText(chefServiceLocationSaveDTO.getCity())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "city 不能为空");
        }
        if (!StringUtils.hasText(chefServiceLocationSaveDTO.getDistrict())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "district 不能为空");
        }
        if (!StringUtils.hasText(chefServiceLocationSaveDTO.getDetailAddress())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "detailAddress 不能为空");
        }
        if (chefServiceLocationSaveDTO.getLongitude() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "longitude 不能为空");
        }
        if (chefServiceLocationSaveDTO.getLatitude() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "latitude 不能为空");
        }
    }

    private ChefServiceLocationVO toChefServiceLocationVO(ChefServiceLocation chefServiceLocation) {
        if (chefServiceLocation == null) {
            return null;
        }
        return ChefServiceLocationVO.builder()
                .id(chefServiceLocation.getId())
                .chefId(chefServiceLocation.getChefId())
                .province(chefServiceLocation.getProvince())
                .city(chefServiceLocation.getCity())
                .district(chefServiceLocation.getDistrict())
                .town(chefServiceLocation.getTown())
                .detailAddress(chefServiceLocation.getDetailAddress())
                .longitude(chefServiceLocation.getLongitude())
                .latitude(chefServiceLocation.getLatitude())
                .createdAt(chefServiceLocation.getCreatedAt())
                .updatedAt(chefServiceLocation.getUpdatedAt())
                .build();
    }
}
