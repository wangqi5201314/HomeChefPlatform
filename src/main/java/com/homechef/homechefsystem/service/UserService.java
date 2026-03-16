package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.UserUpdateDTO;
import com.homechef.homechefsystem.vo.UserVO;

public interface UserService {

    UserVO getById(Long id);

    UserVO getCurrentUser();

    UserVO updateCurrentUser(UserUpdateDTO userUpdateDTO);
}
