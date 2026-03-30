package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ChefCertStatusEnum;
import com.homechef.homechefsystem.common.enums.ChefServiceModeEnum;
import com.homechef.homechefsystem.common.enums.ChefStatusEnum;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.ChefChangePasswordDTO;
import com.homechef.homechefsystem.dto.ChefLoginDTO;
import com.homechef.homechefsystem.dto.ChefQueryDTO;
import com.homechef.homechefsystem.dto.ChefRegisterDTO;
import com.homechef.homechefsystem.dto.ChefUpdateDTO;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.entity.ChefServiceLocation;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.ChefServiceLocationMapper;
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
    private final ChefServiceLocationMapper chefServiceLocationMapper;
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

        validateServiceMode(chefUpdateDTO.getServiceMode());

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
        if (chefUpdateDTO.getServiceMode() != null) {
            existingChef.setServiceMode(chefUpdateDTO.getServiceMode());
        }
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
        if (chef.getStatus() == null || !ChefStatusEnum.NORMAL.getCode().equals(chef.getStatus())) {
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
    public ChefVO register(ChefRegisterDTO chefRegisterDTO) {
        validateRegister(chefRegisterDTO);

        if (chefMapper.selectByPhone(chefRegisterDTO.getPhone()) != null) {
            throw new BusinessException(ResultCodeEnum.FAIL, "phone already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        Chef chef = Chef.builder()
                .name(buildChefName(chefRegisterDTO.getPhone(), chefRegisterDTO.getName()))
                .phone(chefRegisterDTO.getPhone())
                .password(passwordEncoder.encode(chefRegisterDTO.getPassword()))
                .avatar("")
                .gender(0)
                .age(0)
                .introduction("")
                .specialtyCuisine("")
                .specialtyTags("")
                .yearsOfExperience(0)
                .serviceRadiusKm(0)
                .serviceMode(ChefServiceModeEnum.USER_PREPARES_INGREDIENTS.getCode())
                .ratingAvg(null)
                .orderCount(0)
                .onTimeRate(null)
                .goodReviewRate(null)
                .certStatus(ChefCertStatusEnum.PENDING.getCode())
                .status(ChefStatusEnum.NORMAL.getCode())
                .createdAt(now)
                .updatedAt(now)
                .build();

        int rows = chefMapper.insert(chef);
        if (rows <= 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "register failed");
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

        validateServiceMode(chefUpdateDTO.getServiceMode());

        existingChef.setName(chefUpdateDTO.getName());
        existingChef.setAvatar(chefUpdateDTO.getAvatar());
        existingChef.setGender(chefUpdateDTO.getGender());
        existingChef.setAge(chefUpdateDTO.getAge());
        existingChef.setIntroduction(chefUpdateDTO.getIntroduction());
        existingChef.setSpecialtyCuisine(chefUpdateDTO.getSpecialtyCuisine());
        existingChef.setSpecialtyTags(chefUpdateDTO.getSpecialtyTags());
        existingChef.setYearsOfExperience(chefUpdateDTO.getYearsOfExperience());
        existingChef.setServiceRadiusKm(chefUpdateDTO.getServiceRadiusKm());
        if (chefUpdateDTO.getServiceMode() != null) {
            existingChef.setServiceMode(chefUpdateDTO.getServiceMode());
        }
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

    private void validateRegister(ChefRegisterDTO chefRegisterDTO) {
        if (!chefRegisterDTO.getPassword().equals(chefRegisterDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "confirmPassword does not match password");
        }
    }

    private String buildChefName(String phone, String name) {
        if (StringUtils.hasText(name)) {
            return name.trim();
        }
        if (phone != null && phone.length() >= 4) {
            return "厨师" + phone.substring(phone.length() - 4);
        }
        return phone;
    }

    private void validateServiceMode(Integer serviceMode) {
        if (serviceMode != null && !ChefServiceModeEnum.isValid(serviceMode)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "serviceMode 取值非法，只能为 1、2、3");
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
                .certStatusDesc(ChefCertStatusEnum.getDescByCode(chef.getCertStatus()))
                .status(chef.getStatus())
                .build();
    }

    private ChefDetailVO toChefDetailVO(Chef chef) {
        if (chef == null) {
            return null;
        }
        ChefServiceLocation chefServiceLocation = chefServiceLocationMapper.selectByChefId(chef.getId());
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
                .serviceAreaText(buildServiceAreaText(chefServiceLocation))
                .serviceMode(chef.getServiceMode())
                .serviceModeDesc(ChefServiceModeEnum.getDescByCode(chef.getServiceMode()))
                .ratingAvg(chef.getRatingAvg())
                .orderCount(chef.getOrderCount())
                .onTimeRate(chef.getOnTimeRate())
                .goodReviewRate(chef.getGoodReviewRate())
                .certStatus(chef.getCertStatus())
                .certStatusDesc(ChefCertStatusEnum.getDescByCode(chef.getCertStatus()))
                .status(chef.getStatus())
                .statusDesc(ChefStatusEnum.getDescByCode(chef.getStatus()))
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
                .serviceModeDesc(ChefServiceModeEnum.getDescByCode(chef.getServiceMode()))
                .ratingAvg(chef.getRatingAvg())
                .orderCount(chef.getOrderCount())
                .onTimeRate(chef.getOnTimeRate())
                .goodReviewRate(chef.getGoodReviewRate())
                .certStatus(chef.getCertStatus())
                .certStatusDesc(ChefCertStatusEnum.getDescByCode(chef.getCertStatus()))
                .status(chef.getStatus())
                .statusDesc(ChefStatusEnum.getDescByCode(chef.getStatus()))
                .build();
    }

    private String buildServiceAreaText(ChefServiceLocation chefServiceLocation) {
        if (chefServiceLocation == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        appendAreaPart(builder, chefServiceLocation.getProvince());
        appendAreaPart(builder, chefServiceLocation.getCity());
        appendAreaPart(builder, chefServiceLocation.getDistrict());
        appendAreaPart(builder, chefServiceLocation.getTown());
        return builder.length() == 0 ? null : builder.toString();
    }

    private void appendAreaPart(StringBuilder builder, String areaPart) {
        if (StringUtils.hasText(areaPart)) {
            builder.append(areaPart.trim());
        }
    }
}
