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
import com.homechef.homechefsystem.entity.User;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.ChefServiceLocationMapper;
import com.homechef.homechefsystem.mapper.UserMapper;
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
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    /**
     * 查询列表数据并返回结果。
     */
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
    /**
     * 根据 ID 查询对应数据。
     */
    public ChefDetailVO getById(Long id) {
        return toChefDetailVO(chefMapper.selectById(id));
    }

    @Override
    /**
     * 根据 ID 更新数据。
     */
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
    /**
     * 执行登录校验并返回登录结果。
     */
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
    /**
     * 执行注册流程并返回注册结果。
     */
    public ChefVO register(ChefRegisterDTO chefRegisterDTO) {
        validateRegister(chefRegisterDTO);
        ensurePhoneAvailable(chefRegisterDTO.getPhone(), null);

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
                .certStatus(ChefCertStatusEnum.WAIT_UPLOAD.getCode())
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
    /**
     * 获取当前登录主体的资料信息。
     */
    public ChefVO getCurrentChef() {
        Long currentChefId = LoginUserContext.getChefId();
        if (currentChefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return toChefVO(chefMapper.selectById(currentChefId));
    }

    @Override
    /**
     * 更新当前登录主体的资料信息。
     */
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
        applyPhoneIfPresent(existingChef, chefUpdateDTO.getPhone());

        existingChef.setName(chefUpdateDTO.getName());
        existingChef.setPhone(existingChef.getPhone());
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

    /**
     * 按需将输入值应用到目标对象。
     */
    private void applyPhoneIfPresent(Chef existingChef, String phone) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        String normalizedPhone = phone.trim();
        ensurePhoneAvailable(normalizedPhone, existingChef.getId());
        existingChef.setPhone(normalizedPhone);
    }

    /**
     * 校验并确保当前业务条件成立。
     */
    private void ensurePhoneAvailable(String phone, Long currentChefId) {
        if (!StringUtils.hasText(phone)) {
            return;
        }

        String normalizedPhone = phone.trim();
        Chef chefPhoneOwner = chefMapper.selectByPhone(normalizedPhone);
        if (chefPhoneOwner != null && (currentChefId == null || !chefPhoneOwner.getId().equals(currentChefId))) {
            throw new BusinessException(ResultCodeEnum.FAIL, "phone already exists");
        }

        User userPhoneOwner = userMapper.selectByPhone(normalizedPhone);
        if (userPhoneOwner != null) {
            throw new BusinessException(ResultCodeEnum.FAIL, "phone already exists");
        }

        User emergencyPhoneOwner = userMapper.selectByEmergencyContactPhone(normalizedPhone);
        if (emergencyPhoneOwner != null) {
            throw new BusinessException(ResultCodeEnum.FAIL, "phone already exists");
        }
    }

    @Override
    /**
     * 修改当前主体的登录密码。
     */
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

    /**
     * 校验输入参数或业务状态是否合法。
     */
    private void validateRegister(ChefRegisterDTO chefRegisterDTO) {
        if (!chefRegisterDTO.getPassword().equals(chefRegisterDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "confirmPassword does not match password");
        }
    }

    /**
     * 生成厨师展示名称。
     */
    private String buildChefName(String phone, String name) {
        if (StringUtils.hasText(name)) {
            return name.trim();
        }
        if (phone != null && phone.length() >= 4) {
            return "厨师" + phone.substring(phone.length() - 4);
        }
        return phone;
    }

    /**
     * 校验输入参数或业务状态是否合法。
     */
    private void validateServiceMode(Integer serviceMode) {
        if (serviceMode != null && !ChefServiceModeEnum.isValid(serviceMode)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "serviceMode 取值非法，只能为 1、2、3");
        }
    }

    /**
     * 将实体对象转换为前端返回 VO。
     */
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

    /**
     * 将实体对象转换为前端返回 VO。
     */
    private ChefDetailVO toChefDetailVO(Chef chef) {
        if (chef == null) {
            return null;
        }
        ChefServiceLocation chefServiceLocation = chefServiceLocationMapper.selectActiveByChefId(chef.getId());
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
                .serviceAreaProvince(getServiceAreaProvince(chefServiceLocation))
                .serviceAreaCity(getServiceAreaCity(chefServiceLocation))
                .serviceAreaDistrict(getServiceAreaDistrict(chefServiceLocation))
                .serviceAreaTown(getServiceAreaTown(chefServiceLocation))
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

    /**
     * 处理 g et se rv ic ea re ap ro vi nc e 相关逻辑。
     */
    private String getServiceAreaProvince(ChefServiceLocation chefServiceLocation) {
        return chefServiceLocation == null ? null : chefServiceLocation.getProvince();
    }

    /**
     * 处理 g et se rv ic ea re ac it y 相关逻辑。
     */
    private String getServiceAreaCity(ChefServiceLocation chefServiceLocation) {
        return chefServiceLocation == null ? null : chefServiceLocation.getCity();
    }

    /**
     * 处理 g et se rv ic ea re ad is tr ic t 相关逻辑。
     */
    private String getServiceAreaDistrict(ChefServiceLocation chefServiceLocation) {
        return chefServiceLocation == null ? null : chefServiceLocation.getDistrict();
    }

    /**
     * 处理 g et se rv ic ea re at ow n 相关逻辑。
     */
    private String getServiceAreaTown(ChefServiceLocation chefServiceLocation) {
        return chefServiceLocation == null ? null : chefServiceLocation.getTown();
    }

    /**
     * 将实体对象转换为前端返回 VO。
     */
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

    /**
     * 拼接服务区域的文本摘要。
     */
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

    /**
     * 向区域摘要中追加一段非空文本。
     */
    private void appendAreaPart(StringBuilder builder, String areaPart) {
        if (StringUtils.hasText(areaPart)) {
            builder.append(areaPart.trim());
        }
    }
}
