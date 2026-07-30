package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.enums.UserStatusEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.UserChangePasswordDTO;
import com.homechef.homechefsystem.dto.EmailLoginDTO;
import com.homechef.homechefsystem.dto.UserLoginDTO;
import com.homechef.homechefsystem.dto.UserRegisterDTO;
import com.homechef.homechefsystem.dto.UserUpdateDTO;
import com.homechef.homechefsystem.dto.UserWechatLoginDTO;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.entity.User;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.UserMapper;
import com.homechef.homechefsystem.service.UserService;
import com.homechef.homechefsystem.service.EmailVerificationService;
import com.homechef.homechefsystem.service.WechatMiniProgramService;
import com.homechef.homechefsystem.utils.LoginUserContext;
import com.homechef.homechefsystem.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final ChefMapper chefMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final WechatMiniProgramService wechatMiniProgramService;
    private final EmailVerificationService emailVerificationService;

    /**
     * 处理一次登录请求。
     * 这个方法负责校验身份信息，并在登录成功后返回系统需要的登录结果。
     * 它会先查账号或第三方身份，再检查状态和密码，最后生成登录返回数据。
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

    @Override
    public UserVO loginByEmail(EmailLoginDTO emailLoginDTO) {
        String email = normalizeEmail(emailLoginDTO.getEmail());
        emailVerificationService.verifyLoginCode(email, emailLoginDTO.getCode());

        User user = userMapper.selectByEmail(email);
        if (user == null) {
            user = createEmailUser(email);
        } else {
            validateUserForLogin(user);
        }

        return finishLogin(user);
    }

    /**
     * 处理一次登录请求。
     * 这个方法负责校验身份信息，并在登录成功后返回系统需要的登录结果。
     * 它会先查账号或第三方身份，再检查状态和密码，最后生成登录返回数据。
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
     * 完成一次新账号注册。
     * 这个方法主要负责把注册信息转成系统里的正式用户或厨师数据。
     * 它会先做重复和格式检查，再保存数据并返回注册结果。
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
     * 修改当前账号的登录密码。
     * 这个方法让用户、厨师或管理员可以安全地更新自己的密码。
     * 它会先检查旧密码是否正确，再校验新密码，最后把加密后的新密码保存起来。
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
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public UserVO getById(Long id) {
        return toUserVO(userMapper.selectById(id));
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
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
     * 修改一条已有的业务数据。
     * 这个方法用于更新详情信息、状态或某些可编辑字段。
     * 它会先查出原始数据，再把新值写进去，最后保存并返回更新后的结果。
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
     * 把一个可选参数按规则写回到目标对象里。
     * 这个方法主要是为了减少重复的判空和赋值代码。
     * 它会先判断参数有没有值，有值时再更新到目标对象上。
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
     * 确认当前数据状态是否满足继续执行的要求。
     * 这个方法的作用，是把状态判断集中起来，避免主流程里到处写 if 判断。
     * 它会检查状态是否和预期一致，如果不一致，就直接抛出业务异常。
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
     * 检查当前传入的参数或业务状态是否合法。
     * 这个方法的作用，是把不合条件的情况尽早拦住，不让错误数据继续往下执行。
     * 它会根据规则逐项检查参数或状态，只要发现不满足条件，就直接抛出异常。
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
     * 新建一条业务数据。
     * 这个方法用于把前端提交的新信息正式写入数据库。
     * 它会先做必要的检查和组装，再保存数据，最后返回新建后的结果。
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

    private User createEmailUser(String email) {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .email(email)
                .phone(null)
                .password(null)
                .nickname("Email user")
                .avatar("")
                .gender(0)
                .status(UserStatusEnum.NORMAL.getCode())
                .lastLoginTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        int rows = userMapper.insert(user);
        if (rows <= 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "email register failed");
        }
        User createdUser = userMapper.selectById(user.getId());
        if (createdUser == null) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "email register failed");
        }
        return createdUser;
    }

    /**
     * 处理 finishLogin 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
    private UserVO finishLogin(User user) {
        LocalDateTime now = LocalDateTime.now();
        userMapper.updateLoginTimeById(user.getId(), now, now);
        return toUserVO(userMapper.selectById(user.getId()));
    }

    /**
     * 检查当前传入的参数或业务状态是否合法。
     * 这个方法的作用，是把不合条件的情况尽早拦住，不让错误数据继续往下执行。
     * 它会根据规则逐项检查参数或状态，只要发现不满足条件，就直接抛出异常。
     */
    private void validateUserForLogin(User user) {
        if (user.getStatus() == null || !UserStatusEnum.NORMAL.getCode().equals(user.getStatus())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "user is disabled");
        }
    }

    /**
     * 检查当前传入的参数或业务状态是否合法。
     * 这个方法的作用，是把不合条件的情况尽早拦住，不让错误数据继续往下执行。
     * 它会根据规则逐项检查参数或状态，只要发现不满足条件，就直接抛出异常。
     */
    private void validateRegister(UserRegisterDTO userRegisterDTO) {
        if (!userRegisterDTO.getPassword().equals(userRegisterDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "confirmPassword does not match password");
        }
    }

    /**
     * 检查当前传入的参数或业务状态是否合法。
     * 这个方法的作用，是把不合条件的情况尽早拦住，不让错误数据继续往下执行。
     * 它会根据规则逐项检查参数或状态，只要发现不满足条件，就直接抛出异常。
     */
    private void validateChangePassword(UserChangePasswordDTO userChangePasswordDTO) {
        if (!userChangePasswordDTO.getNewPassword().equals(userChangePasswordDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "confirmPassword does not match newPassword");
        }
    }

    /**
     * 构建一个后续会被重复使用的中间结果。
     * 这个方法主要是为了把主流程里的细节拆出去，让主流程更容易看。
     * 它会根据当前需要把集合、映射、路径、文本或比较器等内容先准备好。
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

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 构建一个后续会被重复使用的中间结果。
     * 这个方法主要是为了把主流程里的细节拆出去，让主流程更容易看。
     * 它会根据当前需要把集合、映射、路径、文本或比较器等内容先准备好。
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
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
     */
    private UserVO toUserVO(User user) {
        if (user == null) {
            return null;
        }
        return UserVO.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
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
