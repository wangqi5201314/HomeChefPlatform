package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.UserChangePasswordDTO;
import com.homechef.homechefsystem.dto.UserLoginDTO;
import com.homechef.homechefsystem.dto.UserRegisterDTO;
import com.homechef.homechefsystem.dto.UserUpdateDTO;
import com.homechef.homechefsystem.dto.UserWechatLoginDTO;
import com.homechef.homechefsystem.vo.UserVO;

public interface UserService {

    UserVO login(UserLoginDTO userLoginDTO);

    UserVO loginByWechat(UserWechatLoginDTO userWechatLoginDTO);

    UserVO register(UserRegisterDTO userRegisterDTO);

    void changePassword(UserChangePasswordDTO userChangePasswordDTO);

    UserVO getById(Long id);

    UserVO getCurrentUser();

    UserVO updateCurrentUser(UserUpdateDTO userUpdateDTO);
}
