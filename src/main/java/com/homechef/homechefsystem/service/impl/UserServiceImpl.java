package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.enums.UserStatusEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.UserChangePasswordDTO;
import com.homechef.homechefsystem.dto.UserLoginDTO;
import com.homechef.homechefsystem.dto.UserRegisterDTO;
import com.homechef.homechefsystem.dto.UserUpdateDTO;
import com.homechef.homechefsystem.dto.UserWechatLoginDTO;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.entity.User;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.UserMapper;
import com.homechef.homechefsystem.service.UserService;
import com.homechef.homechefsystem.service.WechatMiniProgramService;
import com.homechef.homechefsystem.utils.LoginUserContext;
import com.homechef.homechefsystem.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final ChefMapper chefMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final WechatMiniProgramService wechatMiniProgramService;

    /**
     * 方法说明：在 用户服务实现 中处理 login 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
    public UserVO login(UserLoginDTO userLoginDTO) {
        User user = userMapper.selectByPhone(userLoginDTO.getPhone());
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "phone or password is incorrect");
        }
        validateUserForLogin(user);
        if (!StringUtils.hasText(user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "password is not set");
        }
        if (!passwordEncoder.matches(userLoginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "phone or password is incorrect");
        }

        return finishLogin(user);
    }

    /**
     * 方法说明：通过微信小程序登录态完成平台用户登录或自动注册。
     * 主要作用：它让用户可以用微信 code 直接进入系统，减少传统账号密码登录门槛。
     * 实现逻辑：方法会先调用微信登录服务换取 openid，再按 openid 查询本地用户；若用户不存在则自动创建，最后统一执行登录收尾并返回 token。
     */
    @Override
    public UserVO loginByWechat(UserWechatLoginDTO userWechatLoginDTO) {
        WechatMiniProgramService.WechatLoginInfo wechatLoginInfo =
                wechatMiniProgramService.code2Session(userWechatLoginDTO.getCode());

        User user = userMapper.selectByOpenid(wechatLoginInfo.openid());
        if (user == null) {
            user = createWechatUser(wechatLoginInfo);
        } else {
            validateUserForLogin(user);
        }

        return finishLogin(user);
    }

    /**
     * 方法说明：在 用户服务实现 中处理 register 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
    public UserVO register(UserRegisterDTO userRegisterDTO) {
        validateRegister(userRegisterDTO);
        ensurePhoneAvailable(userRegisterDTO.getPhone(), null);

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .phone(userRegisterDTO.getPhone())
                .password(passwordEncoder.encode(userRegisterDTO.getPassword()))
                .nickname(buildPhoneNickname(userRegisterDTO.getPhone(), userRegisterDTO.getNickname()))
                .avatar("")
                .gender(0)
                .status(UserStatusEnum.NORMAL.getCode())
                .createdAt(now)
                .updatedAt(now)
                .build();

        int rows = userMapper.insert(user);
        if (rows <= 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "register failed");
        }
        return toUserVO(userMapper.selectById(user.getId()));
    }

    /**
     * 方法说明：在 用户服务实现 中处理 changePassword 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
    public void changePassword(UserChangePasswordDTO userChangePasswordDTO) {
        Long currentUserId = LoginUserContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }

        validateChangePassword(userChangePasswordDTO);

        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "user not found");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.FAIL, "password is not set");
        }
        if (!passwordEncoder.matches(userChangePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.FAIL, "old password is incorrect");
        }

        int rows = userMapper.updatePasswordById(
                currentUserId,
                passwordEncoder.encode(userChangePasswordDTO.getNewPassword()),
                LocalDateTime.now()
        );
        if (rows <= 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "change password failed");
        }
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 用户服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public UserVO getById(Long id) {
        return toUserVO(userMapper.selectById(id));
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 用户服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public UserVO getCurrentUser() {
        Long currentUserId = LoginUserContext.getUserId();
        if (currentUserId == null) {
            return null;
        }
        return getById(currentUserId);
    }

    /**
     * 方法说明：修改当前登录用户的个人资料。
     * 主要作用：它把昵称、头像、手机号和紧急联系人等个人信息更新集中到一个入口，方便前端统一提交。
     * 实现逻辑：方法会先查询当前用户，再分别处理手机号和紧急联系人手机号的唯一性校验，最后更新实体并返回最新用户资料。
     */
    @Override
    public UserVO updateCurrentUser(UserUpdateDTO userUpdateDTO) {
        Long currentUserId = LoginUserContext.getUserId();
        if (currentUserId == null) {
            return null;
        }

        User existingUser = userMapper.selectById(currentUserId);
        if (existingUser == null) {
            return null;
        }

        applyPhoneIfPresent(existingUser, userUpdateDTO.getPhone());
        existingUser.setNickname(userUpdateDTO.getNickname());
        existingUser.setAvatar(userUpdateDTO.getAvatar());
        existingUser.setGender(userUpdateDTO.getGender());
        existingUser.setBirthday(userUpdateDTO.getBirthday());
        existingUser.setTastePreference(userUpdateDTO.getTastePreference());
        existingUser.setAllergyInfo(userUpdateDTO.getAllergyInfo());
        existingUser.setEmergencyContactName(userUpdateDTO.getEmergencyContactName());
        validateEmergencyContactPhone(userUpdateDTO.getEmergencyContactPhone(), existingUser.getId(), existingUser.getPhone());
        existingUser.setEmergencyContactPhone(userUpdateDTO.getEmergencyContactPhone());
        existingUser.setUpdatedAt(LocalDateTime.now());

        int rows = userMapper.updateProfileById(existingUser);
        if (rows <= 0) {
            return null;
        }
        return toUserVO(userMapper.selectById(currentUserId));
    }

    /**
     * 方法说明：在 用户服务实现 中处理 applyPhoneIfPresent 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    private void applyPhoneIfPresent(User existingUser, String phone) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        String normalizedPhone = phone.trim();
        ensurePhoneAvailable(normalizedPhone, existingUser.getId());
        existingUser.setPhone(normalizedPhone);
    }

    /**
     * 方法说明：确保当前业务状态满足继续执行的要求。
     * 主要作用：它用于把 用户服务实现 中必须成立的约束条件显式收口，避免非法状态继续向后流转。
     * 实现逻辑：实现时会读取当前对象的关键状态或字段，并与目标要求进行比较；若不满足则立即抛出业务异常。
     */
    private void ensurePhoneAvailable(String phone, Long currentUserId) {
        if (!StringUtils.hasText(phone)) {
            return;
        }

        String normalizedPhone = phone.trim();
        User userPhoneOwner = userMapper.selectByPhone(normalizedPhone);
        if (userPhoneOwner != null && (currentUserId == null || !userPhoneOwner.getId().equals(currentUserId))) {
            throw new BusinessException(ResultCodeEnum.FAIL, "phone already exists");
        }

        Chef chefPhoneOwner = chefMapper.selectByPhone(normalizedPhone);
        if (chefPhoneOwner != null) {
            throw new BusinessException(ResultCodeEnum.FAIL, "phone already exists");
        }

        User emergencyPhoneOwner = userMapper.selectByEmergencyContactPhone(normalizedPhone);
        if (emergencyPhoneOwner != null && (currentUserId == null || !emergencyPhoneOwner.getId().equals(currentUserId))) {
            throw new BusinessException(ResultCodeEnum.FAIL, "phone already exists");
        }
    }

    /**
     * 方法说明：校验当前业务输入或状态是否满足执行条件。
     * 主要作用：它用于把 用户服务实现 中的前置规则集中收口，避免核心流程夹杂过多重复的条件判断。
     * 实现逻辑：实现逻辑会逐项检查关键字段、状态或业务约束，一旦发现不满足条件的情况就立即抛出业务异常阻断流程。
     */
    private void validateEmergencyContactPhone(String emergencyContactPhone, Long currentUserId, String currentUserPhone) {
        if (!StringUtils.hasText(emergencyContactPhone)) {
            return;
        }

        String normalizedEmergencyContactPhone = emergencyContactPhone.trim();
        if (StringUtils.hasText(currentUserPhone) && normalizedEmergencyContactPhone.equals(currentUserPhone.trim())) {
            throw new BusinessException(ResultCodeEnum.FAIL, "emergencyContactPhone already exists");
        }

        User userPhoneOwner = userMapper.selectByPhone(normalizedEmergencyContactPhone);
        if (userPhoneOwner != null && (currentUserId == null || !userPhoneOwner.getId().equals(currentUserId))) {
            throw new BusinessException(ResultCodeEnum.FAIL, "emergencyContactPhone already exists");
        }

        Chef chefPhoneOwner = chefMapper.selectByPhone(normalizedEmergencyContactPhone);
        if (chefPhoneOwner != null) {
            throw new BusinessException(ResultCodeEnum.FAIL, "emergencyContactPhone already exists");
        }

        User emergencyPhoneOwner = userMapper.selectByEmergencyContactPhone(normalizedEmergencyContactPhone);
        if (emergencyPhoneOwner != null && (currentUserId == null || !emergencyPhoneOwner.getId().equals(currentUserId))) {
            throw new BusinessException(ResultCodeEnum.FAIL, "emergencyContactPhone already exists");
        }
    }

    /**
     * 方法说明：新增一条当前业务场景下的数据记录。
     * 主要作用：它承担 用户服务实现 中的新增入口，把前端入参转换为可持久化的实体数据。
     * 实现逻辑：实现逻辑通常会先校验关键字段和归属关系，再组装实体写入数据库，最后返回新增后的最新结果。
     */
    private User createWechatUser(WechatMiniProgramService.WechatLoginInfo wechatLoginInfo) {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .openid(wechatLoginInfo.openid())
                .unionid(wechatLoginInfo.unionid())
                .phone(null)
                .password(null)
                .nickname(buildWechatNickname(wechatLoginInfo.openid()))
                .avatar("")
                .gender(0)
                .status(UserStatusEnum.NORMAL.getCode())
                .lastLoginTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        int rows = userMapper.insert(user);
        if (rows <= 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "wechat register failed");
        }
        User createdUser = userMapper.selectById(user.getId());
        if (createdUser == null) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "wechat register failed");
        }
        return createdUser;
    }

    /**
     * 方法说明：在 用户服务实现 中处理 finishLogin 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    private UserVO finishLogin(User user) {
        LocalDateTime now = LocalDateTime.now();
        userMapper.updateLoginTimeById(user.getId(), now, now);
        return toUserVO(userMapper.selectById(user.getId()));
    }

    /**
     * 方法说明：校验当前业务输入或状态是否满足执行条件。
     * 主要作用：它用于把 用户服务实现 中的前置规则集中收口，避免核心流程夹杂过多重复的条件判断。
     * 实现逻辑：实现逻辑会逐项检查关键字段、状态或业务约束，一旦发现不满足条件的情况就立即抛出业务异常阻断流程。
     */
    private void validateUserForLogin(User user) {
        if (user.getStatus() == null || !UserStatusEnum.NORMAL.getCode().equals(user.getStatus())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "user is disabled");
        }
    }

    /**
     * 方法说明：校验当前业务输入或状态是否满足执行条件。
     * 主要作用：它用于把 用户服务实现 中的前置规则集中收口，避免核心流程夹杂过多重复的条件判断。
     * 实现逻辑：实现逻辑会逐项检查关键字段、状态或业务约束，一旦发现不满足条件的情况就立即抛出业务异常阻断流程。
     */
    private void validateRegister(UserRegisterDTO userRegisterDTO) {
        if (!userRegisterDTO.getPassword().equals(userRegisterDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "confirmPassword does not match password");
        }
    }

    /**
     * 方法说明：校验当前业务输入或状态是否满足执行条件。
     * 主要作用：它用于把 用户服务实现 中的前置规则集中收口，避免核心流程夹杂过多重复的条件判断。
     * 实现逻辑：实现逻辑会逐项检查关键字段、状态或业务约束，一旦发现不满足条件的情况就立即抛出业务异常阻断流程。
     */
    private void validateChangePassword(UserChangePasswordDTO userChangePasswordDTO) {
        if (!userChangePasswordDTO.getNewPassword().equals(userChangePasswordDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "confirmPassword does not match newPassword");
        }
    }

    /**
     * 方法说明：构建当前业务流程后续需要复用的中间结果。
     * 主要作用：这个辅助方法把 用户服务实现 中重复使用的数据结构提前整理好，减少主流程中的重复计算和分支判断。
     * 实现逻辑：实现逻辑通常会对输入参数做空值保护，再根据业务规则拼装映射、集合、文本或比较器，供后续步骤直接复用。
     */
    private String buildPhoneNickname(String phone, String nickname) {
        if (StringUtils.hasText(nickname)) {
            return nickname.trim();
        }
        if (phone != null && phone.length() >= 4) {
            return "用户" + phone.substring(phone.length() - 4);
        }
        return phone;
    }

    /**
     * 方法说明：构建当前业务流程后续需要复用的中间结果。
     * 主要作用：这个辅助方法把 用户服务实现 中重复使用的数据结构提前整理好，减少主流程中的重复计算和分支判断。
     * 实现逻辑：实现逻辑通常会对输入参数做空值保护，再根据业务规则拼装映射、集合、文本或比较器，供后续步骤直接复用。
     */
    private String buildWechatNickname(String openid) {
        if (!StringUtils.hasText(openid)) {
            return "微信用户";
        }
        if (openid.length() <= 6) {
            return "微信用户" + openid;
        }
        return "微信用户" + openid.substring(openid.length() - 6);
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 用户服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
    private UserVO toUserVO(User user) {
        if (user == null) {
            return null;
        }
        return UserVO.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .birthday(user.getBirthday())
                .tastePreference(user.getTastePreference())
                .allergyInfo(user.getAllergyInfo())
                .emergencyContactName(user.getEmergencyContactName())
                .emergencyContactPhone(user.getEmergencyContactPhone())
                .status(user.getStatus())
                .statusDesc(UserStatusEnum.getDescByCode(user.getStatus()))
                .build();
    }
}
