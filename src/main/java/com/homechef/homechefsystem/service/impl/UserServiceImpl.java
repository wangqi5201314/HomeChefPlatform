package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.enums.UserStatusEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.UserChangePasswordDTO;
import com.homechef.homechefsystem.dto.UserLoginDTO;
import com.homechef.homechefsystem.dto.UserRegisterDTO;
import com.homechef.homechefsystem.dto.UserUpdateDTO;
import com.homechef.homechefsystem.dto.UserWechatLoginDTO;
import com.homechef.homechefsystem.entity.User;
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
    private final BCryptPasswordEncoder passwordEncoder;
    private final WechatMiniProgramService wechatMiniProgramService;

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

    @Override
    public UserVO register(UserRegisterDTO userRegisterDTO) {
        validateRegister(userRegisterDTO);

        if (userMapper.selectByPhone(userRegisterDTO.getPhone()) != null) {
            throw new BusinessException(ResultCodeEnum.FAIL, "phone already exists");
        }

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

    @Override
    public UserVO getById(Long id) {
        return toUserVO(userMapper.selectById(id));
    }

    @Override
    public UserVO getCurrentUser() {
        Long currentUserId = LoginUserContext.getUserId();
        if (currentUserId == null) {
            return null;
        }
        return getById(currentUserId);
    }

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

        existingUser.setNickname(userUpdateDTO.getNickname());
        existingUser.setAvatar(userUpdateDTO.getAvatar());
        existingUser.setGender(userUpdateDTO.getGender());
        existingUser.setBirthday(userUpdateDTO.getBirthday());
        existingUser.setTastePreference(userUpdateDTO.getTastePreference());
        existingUser.setAllergyInfo(userUpdateDTO.getAllergyInfo());
        existingUser.setEmergencyContactName(userUpdateDTO.getEmergencyContactName());
        existingUser.setEmergencyContactPhone(userUpdateDTO.getEmergencyContactPhone());
        existingUser.setUpdatedAt(LocalDateTime.now());

        int rows = userMapper.updateProfileById(existingUser);
        if (rows <= 0) {
            return null;
        }
        return toUserVO(userMapper.selectById(currentUserId));
    }

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

    private UserVO finishLogin(User user) {
        LocalDateTime now = LocalDateTime.now();
        userMapper.updateLoginTimeById(user.getId(), now, now);
        return toUserVO(userMapper.selectById(user.getId()));
    }

    private void validateUserForLogin(User user) {
        if (user.getStatus() == null || !UserStatusEnum.NORMAL.getCode().equals(user.getStatus())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "user is disabled");
        }
    }

    private void validateRegister(UserRegisterDTO userRegisterDTO) {
        if (!userRegisterDTO.getPassword().equals(userRegisterDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "confirmPassword does not match password");
        }
    }

    private void validateChangePassword(UserChangePasswordDTO userChangePasswordDTO) {
        if (!userChangePasswordDTO.getNewPassword().equals(userChangePasswordDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "confirmPassword does not match newPassword");
        }
    }

    private String buildPhoneNickname(String phone, String nickname) {
        if (StringUtils.hasText(nickname)) {
            return nickname.trim();
        }
        if (phone != null && phone.length() >= 4) {
            return "用户" + phone.substring(phone.length() - 4);
        }
        return phone;
    }

    private String buildWechatNickname(String openid) {
        if (!StringUtils.hasText(openid)) {
            return "微信用户";
        }
        if (openid.length() <= 6) {
            return "微信用户" + openid;
        }
        return "微信用户" + openid.substring(openid.length() - 6);
    }

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
