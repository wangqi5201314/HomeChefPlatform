package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.dto.UserUpdateDTO;
import com.homechef.homechefsystem.entity.User;
import com.homechef.homechefsystem.mapper.UserMapper;
import com.homechef.homechefsystem.service.UserService;
import com.homechef.homechefsystem.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Long MOCK_CURRENT_USER_ID = 1L;

    private final UserMapper userMapper;

    @Override
    public UserVO getById(Long id) {
        return toUserVO(userMapper.selectById(id));
    }

    @Override
    public UserVO getCurrentUser() {
        return getById(MOCK_CURRENT_USER_ID);
    }

    @Override
    public UserVO updateCurrentUser(UserUpdateDTO userUpdateDTO) {
        User existingUser = userMapper.selectById(MOCK_CURRENT_USER_ID);
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
        return toUserVO(userMapper.selectById(MOCK_CURRENT_USER_ID));
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
                .build();
    }
}
