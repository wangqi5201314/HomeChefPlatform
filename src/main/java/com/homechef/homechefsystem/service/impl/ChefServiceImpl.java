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
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
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
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public ChefDetailVO getById(Long id) {
        return toChefDetailVO(chefMapper.selectById(id));
    }

    /**
     * 修改一条已有的业务数据。
     * 这个方法用于更新详情信息、状态或某些可编辑字段。
     * 它会先查出原始数据，再把新值写进去，最后保存并返回更新后的结果。
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
     * 处理一次登录请求。
     * 这个方法负责校验身份信息，并在登录成功后返回系统需要的登录结果。
     * 它会先查账号或第三方身份，再检查状态和密码，最后生成登录返回数据。
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
     * 完成一次新账号注册。
     * 这个方法主要负责把注册信息转成系统里的正式用户或厨师数据。
     * 它会先做重复和格式检查，再保存数据并返回注册结果。
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
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
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
     * 修改一条已有的业务数据。
     * 这个方法用于更新详情信息、状态或某些可编辑字段。
     * 它会先查出原始数据，再把新值写进去，最后保存并返回更新后的结果。
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
     * 把一个可选参数按规则写回到目标对象里。
     * 这个方法主要是为了减少重复的判空和赋值代码。
     * 它会先判断参数有没有值，有值时再更新到目标对象上。
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
     * 确认当前数据状态是否满足继续执行的要求。
     * 这个方法的作用，是把状态判断集中起来，避免主流程里到处写 if 判断。
     * 它会检查状态是否和预期一致，如果不一致，就直接抛出业务异常。
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
     * 修改当前账号的登录密码。
     * 这个方法让用户、厨师或管理员可以安全地更新自己的密码。
     * 它会先检查旧密码是否正确，再校验新密码，最后把加密后的新密码保存起来。
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
     * 检查当前传入的参数或业务状态是否合法。
     * 这个方法的作用，是把不合条件的情况尽早拦住，不让错误数据继续往下执行。
     * 它会根据规则逐项检查参数或状态，只要发现不满足条件，就直接抛出异常。
     */
    private void validateRegister(ChefRegisterDTO chefRegisterDTO) {
        if (!chefRegisterDTO.getPassword().equals(chefRegisterDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "confirmPassword does not match password");
        }
    }

    /**
     * 构建一个后续会被重复使用的中间结果。
     * 这个方法主要是为了把主流程里的细节拆出去，让主流程更容易看。
     * 它会根据当前需要把集合、映射、路径、文本或比较器等内容先准备好。
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
     * 检查当前传入的参数或业务状态是否合法。
     * 这个方法的作用，是把不合条件的情况尽早拦住，不让错误数据继续往下执行。
     * 它会根据规则逐项检查参数或状态，只要发现不满足条件，就直接抛出异常。
     */
    private void validateServiceMode(Integer serviceMode) {
        if (serviceMode != null && !ChefServiceModeEnum.isValid(serviceMode)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "serviceMode 取值非法，只能为 1、2、3");
        }
    }

    /**
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    private String getServiceAreaProvince(ChefServiceLocation chefServiceLocation) {
        return chefServiceLocation == null ? null : chefServiceLocation.getProvince();
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    private String getServiceAreaCity(ChefServiceLocation chefServiceLocation) {
        return chefServiceLocation == null ? null : chefServiceLocation.getCity();
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    private String getServiceAreaDistrict(ChefServiceLocation chefServiceLocation) {
        return chefServiceLocation == null ? null : chefServiceLocation.getDistrict();
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    private String getServiceAreaTown(ChefServiceLocation chefServiceLocation) {
        return chefServiceLocation == null ? null : chefServiceLocation.getTown();
    }

    /**
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
     * 构建一个后续会被重复使用的中间结果。
     * 这个方法主要是为了把主流程里的细节拆出去，让主流程更容易看。
     * 它会根据当前需要把集合、映射、路径、文本或比较器等内容先准备好。
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
     * 处理 appendAreaPart 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
    private void appendAreaPart(StringBuilder builder, String areaPart) {
        if (StringUtils.hasText(areaPart)) {
            builder.append(areaPart.trim());
        }
    }
}
