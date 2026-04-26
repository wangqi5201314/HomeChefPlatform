package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.UserChangePasswordDTO;
import com.homechef.homechefsystem.dto.UserLoginDTO;
import com.homechef.homechefsystem.dto.UserRegisterDTO;
import com.homechef.homechefsystem.dto.UserUpdateDTO;
import com.homechef.homechefsystem.dto.UserWechatLoginDTO;
import com.homechef.homechefsystem.vo.UserVO;

public interface UserService {

    /**
     * 执行登录并返回登录结果。
     */
    UserVO login(UserLoginDTO userLoginDTO);

    /**
     * 使用微信登录信息完成登录。
     */
    UserVO loginByWechat(UserWechatLoginDTO userWechatLoginDTO);

    /**
     * 执行注册并返回注册结果。
     */
    UserVO register(UserRegisterDTO userRegisterDTO);

    /**
     * 修改当前主体的登录密码。
     */
    void changePassword(UserChangePasswordDTO userChangePasswordDTO);

    /**
     * 根据 ID 查询对应数据。
     */
    UserVO getById(Long id);

    /**
     * 获取当前登录主体的资料信息。
     */
    UserVO getCurrentUser();

    /**
     * 更新当前登录主体的资料信息。
     */
    UserVO updateCurrentUser(UserUpdateDTO userUpdateDTO);
}
