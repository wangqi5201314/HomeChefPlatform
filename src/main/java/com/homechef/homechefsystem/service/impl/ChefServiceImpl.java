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

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 厨师资料服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
     */
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

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师资料服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public ChefDetailVO getById(Long id) {
        return toChefDetailVO(chefMapper.selectById(id));
    }

    /**
     * 方法说明：更新一条当前业务场景下的数据记录或状态。
     * 主要作用：该方法用于 厨师资料服务实现 中的编辑、状态变更或流程推进，保证外部只能修改业务允许变动的部分。
     * 实现逻辑：实现时会先查询原始数据并做归属或状态校验，再回填可编辑字段执行更新，必要时返回更新后的详情结果。
     */
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

    /**
     * 方法说明：在 厨师资料服务实现 中处理 login 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
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

    /**
     * 方法说明：在 厨师资料服务实现 中处理 register 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
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

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师资料服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public ChefVO getCurrentChef() {
        Long currentChefId = LoginUserContext.getChefId();
        if (currentChefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return toChefVO(chefMapper.selectById(currentChefId));
    }

    /**
     * 方法说明：修改当前登录厨师的个人资料与服务能力信息。
     * 主要作用：该方法用于厨师端完善资料、更新展示信息和调整服务能力参数，同时保证关键字段符合业务约束。
     * 实现逻辑：实现时会先加载当前厨师记录，再处理手机号唯一性、服务模式合法性等校验，最后更新可编辑字段并返回最新资料。
     */
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
     * 方法说明：在 厨师资料服务实现 中处理 applyPhoneIfPresent 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：确保当前业务状态满足继续执行的要求。
     * 主要作用：它用于把 厨师资料服务实现 中必须成立的约束条件显式收口，避免非法状态继续向后流转。
     * 实现逻辑：实现时会读取当前对象的关键状态或字段，并与目标要求进行比较；若不满足则立即抛出业务异常。
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

    /**
     * 方法说明：在 厨师资料服务实现 中处理 changePassword 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
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

    /**
     * 方法说明：校验当前业务输入或状态是否满足执行条件。
     * 主要作用：它用于把 厨师资料服务实现 中的前置规则集中收口，避免核心流程夹杂过多重复的条件判断。
     * 实现逻辑：实现逻辑会逐项检查关键字段、状态或业务约束，一旦发现不满足条件的情况就立即抛出业务异常阻断流程。
     */
    private void validateRegister(ChefRegisterDTO chefRegisterDTO) {
        if (!chefRegisterDTO.getPassword().equals(chefRegisterDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "confirmPassword does not match password");
        }
    }

    /**
     * 方法说明：构建当前业务流程后续需要复用的中间结果。
     * 主要作用：这个辅助方法把 厨师资料服务实现 中重复使用的数据结构提前整理好，减少主流程中的重复计算和分支判断。
     * 实现逻辑：实现逻辑通常会对输入参数做空值保护，再根据业务规则拼装映射、集合、文本或比较器，供后续步骤直接复用。
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
     * 方法说明：校验当前业务输入或状态是否满足执行条件。
     * 主要作用：它用于把 厨师资料服务实现 中的前置规则集中收口，避免核心流程夹杂过多重复的条件判断。
     * 实现逻辑：实现逻辑会逐项检查关键字段、状态或业务约束，一旦发现不满足条件的情况就立即抛出业务异常阻断流程。
     */
    private void validateServiceMode(Integer serviceMode) {
        if (serviceMode != null && !ChefServiceModeEnum.isValid(serviceMode)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "serviceMode 取值非法，只能为 1、2、3");
        }
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 厨师资料服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 厨师资料服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师资料服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    private String getServiceAreaProvince(ChefServiceLocation chefServiceLocation) {
        return chefServiceLocation == null ? null : chefServiceLocation.getProvince();
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师资料服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    private String getServiceAreaCity(ChefServiceLocation chefServiceLocation) {
        return chefServiceLocation == null ? null : chefServiceLocation.getCity();
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师资料服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    private String getServiceAreaDistrict(ChefServiceLocation chefServiceLocation) {
        return chefServiceLocation == null ? null : chefServiceLocation.getDistrict();
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师资料服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    private String getServiceAreaTown(ChefServiceLocation chefServiceLocation) {
        return chefServiceLocation == null ? null : chefServiceLocation.getTown();
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 厨师资料服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
     * 方法说明：构建当前业务流程后续需要复用的中间结果。
     * 主要作用：这个辅助方法把 厨师资料服务实现 中重复使用的数据结构提前整理好，减少主流程中的重复计算和分支判断。
     * 实现逻辑：实现逻辑通常会对输入参数做空值保护，再根据业务规则拼装映射、集合、文本或比较器，供后续步骤直接复用。
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
     * 方法说明：在 厨师资料服务实现 中处理 appendAreaPart 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    private void appendAreaPart(StringBuilder builder, String areaPart) {
        if (StringUtils.hasText(areaPart)) {
            builder.append(areaPart.trim());
        }
    }
}
