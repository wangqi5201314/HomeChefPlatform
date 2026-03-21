package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.ChefChangePasswordDTO;
import com.homechef.homechefsystem.dto.ChefLoginDTO;
import com.homechef.homechefsystem.dto.ChefQueryDTO;
import com.homechef.homechefsystem.dto.ChefUpdateDTO;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.service.ChefService;
import com.homechef.homechefsystem.utils.LoginUserContext;
import com.homechef.homechefsystem.vo.ChefDetailVO;
import com.homechef.homechefsystem.vo.ChefListVO;
import com.homechef.homechefsystem.vo.ChefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChefServiceImpl implements ChefService {

    private final ChefMapper chefMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public List<ChefListVO> getChefList(ChefQueryDTO queryDTO) {
        List<Chef> chefList = chefMapper.selectList(queryDTO);
        if (chefList == null || chefList.isEmpty()) {
            return Collections.emptyList();
        }
        return chefList.stream()
                .map(this::toChefListVO)
                .collect(Collectors.toList());
    }

    @Override
    public ChefDetailVO getById(Long id) {
        return toChefDetailVO(chefMapper.selectById(id));
    }

    @Override
    public ChefDetailVO updateById(Long id, ChefUpdateDTO chefUpdateDTO) {
        Chef existingChef = chefMapper.selectById(id);
        if (existingChef == null) {
            return null;
        }

        existingChef.setName(chefUpdateDTO.getName());
        existingChef.setPhone(chefUpdateDTO.getPhone());
        existingChef.setAvatar(chefUpdateDTO.getAvatar());
        existingChef.setGender(chefUpdateDTO.getGender());
        existingChef.setAge(chefUpdateDTO.getAge());
        existingChef.setIntroduction(chefUpdateDTO.getIntroduction());
        existingChef.setSpecialtyCuisine(chefUpdateDTO.getSpecialtyCuisine());
        existingChef.setSpecialtyTags(chefUpdateDTO.getSpecialtyTags());
        existingChef.setYearsOfExperience(chefUpdateDTO.getYearsOfExperience());
        existingChef.setServiceRadiusKm(chefUpdateDTO.getServiceRadiusKm());
        existingChef.setServiceMode(chefUpdateDTO.getServiceMode());
        existingChef.setStatus(chefUpdateDTO.getStatus());
        existingChef.setUpdatedAt(LocalDateTime.now());

        int rows = chefMapper.updateById(existingChef);
        if (rows <= 0) {
            return null;
        }
        return toChefDetailVO(chefMapper.selectById(id));
    }

    @Override
    public ChefVO login(ChefLoginDTO chefLoginDTO) {
        Chef chef = chefMapper.selectByPhone(chefLoginDTO.getPhone());
        if (chef == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "phone or password is incorrect");
        }
        if (chef.getStatus() == null || chef.getStatus() != 1) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "chef is disabled");
        }
        if (!StringUtils.hasText(chef.getPassword())) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "password is not set");
        }
        if (!passwordEncoder.matches(chefLoginDTO.getPassword(), chef.getPassword())) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "phone or password is incorrect");
        }
        return toChefVO(chefMapper.selectById(chef.getId()));
    }

    @Override
    public ChefVO getCurrentChef() {
        Long currentChefId = LoginUserContext.getChefId();
        if (currentChefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return toChefVO(chefMapper.selectById(currentChefId));
    }

    @Override
    public ChefVO updateCurrentChef(ChefUpdateDTO chefUpdateDTO) {
        Long currentChefId = LoginUserContext.getChefId();
        if (currentChefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }

        Chef existingChef = chefMapper.selectById(currentChefId);
        if (existingChef == null) {
            return null;
        }

        existingChef.setName(chefUpdateDTO.getName());
        existingChef.setAvatar(chefUpdateDTO.getAvatar());
        existingChef.setGender(chefUpdateDTO.getGender());
        existingChef.setAge(chefUpdateDTO.getAge());
        existingChef.setIntroduction(chefUpdateDTO.getIntroduction());
        existingChef.setSpecialtyCuisine(chefUpdateDTO.getSpecialtyCuisine());
        existingChef.setSpecialtyTags(chefUpdateDTO.getSpecialtyTags());
        existingChef.setYearsOfExperience(chefUpdateDTO.getYearsOfExperience());
        existingChef.setServiceRadiusKm(chefUpdateDTO.getServiceRadiusKm());
        existingChef.setServiceMode(chefUpdateDTO.getServiceMode());
        existingChef.setUpdatedAt(LocalDateTime.now());

        int rows = chefMapper.updateById(existingChef);
        if (rows <= 0) {
            return null;
        }
        return toChefVO(chefMapper.selectById(currentChefId));
    }

    @Override
    public void changePassword(ChefChangePasswordDTO chefChangePasswordDTO) {
        Long currentChefId = LoginUserContext.getChefId();
        if (currentChefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        if (!chefChangePasswordDTO.getNewPassword().equals(chefChangePasswordDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "confirmPassword does not match newPassword");
        }

        Chef chef = chefMapper.selectById(currentChefId);
        if (chef == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "chef not found");
        }
        if (!StringUtils.hasText(chef.getPassword())) {
            throw new BusinessException(ResultCodeEnum.FAIL, "password is not set");
        }
        if (!passwordEncoder.matches(chefChangePasswordDTO.getOldPassword(), chef.getPassword())) {
            throw new BusinessException(ResultCodeEnum.FAIL, "old password is incorrect");
        }

        int rows = chefMapper.updatePasswordById(
                currentChefId,
                passwordEncoder.encode(chefChangePasswordDTO.getNewPassword()),
                LocalDateTime.now()
        );
        if (rows <= 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "change password failed");
        }
    }

    private ChefListVO toChefListVO(Chef chef) {
        if (chef == null) {
            return null;
        }
        return ChefListVO.builder()
                .id(chef.getId())
                .name(chef.getName())
                .avatar(chef.getAvatar())
                .specialtyCuisine(chef.getSpecialtyCuisine())
                .yearsOfExperience(chef.getYearsOfExperience())
                .ratingAvg(chef.getRatingAvg())
                .orderCount(chef.getOrderCount())
                .certStatus(chef.getCertStatus())
                .status(chef.getStatus())
                .build();
    }

    private ChefDetailVO toChefDetailVO(Chef chef) {
        if (chef == null) {
            return null;
        }
        return ChefDetailVO.builder()
                .id(chef.getId())
                .name(chef.getName())
                .phone(chef.getPhone())
                .avatar(chef.getAvatar())
                .gender(chef.getGender())
                .age(chef.getAge())
                .introduction(chef.getIntroduction())
                .specialtyCuisine(chef.getSpecialtyCuisine())
                .specialtyTags(chef.getSpecialtyTags())
                .yearsOfExperience(chef.getYearsOfExperience())
                .serviceRadiusKm(chef.getServiceRadiusKm())
                .serviceMode(chef.getServiceMode())
                .ratingAvg(chef.getRatingAvg())
                .orderCount(chef.getOrderCount())
                .onTimeRate(chef.getOnTimeRate())
                .goodReviewRate(chef.getGoodReviewRate())
                .certStatus(chef.getCertStatus())
                .status(chef.getStatus())
                .build();
    }

    private ChefVO toChefVO(Chef chef) {
        if (chef == null) {
            return null;
        }
        return ChefVO.builder()
                .id(chef.getId())
                .name(chef.getName())
                .phone(chef.getPhone())
                .avatar(chef.getAvatar())
                .gender(chef.getGender())
                .age(chef.getAge())
                .introduction(chef.getIntroduction())
                .specialtyCuisine(chef.getSpecialtyCuisine())
                .specialtyTags(chef.getSpecialtyTags())
                .yearsOfExperience(chef.getYearsOfExperience())
                .serviceRadiusKm(chef.getServiceRadiusKm())
                .serviceMode(chef.getServiceMode())
                .ratingAvg(chef.getRatingAvg())
                .orderCount(chef.getOrderCount())
                .onTimeRate(chef.getOnTimeRate())
                .goodReviewRate(chef.getGoodReviewRate())
                .certStatus(chef.getCertStatus())
                .status(chef.getStatus())
                .build();
    }
}
