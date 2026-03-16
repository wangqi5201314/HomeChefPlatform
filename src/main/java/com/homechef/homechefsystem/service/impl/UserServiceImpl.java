package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.entity.User;
import com.homechef.homechefsystem.mapper.UserMapper;
import com.homechef.homechefsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Long MOCK_CURRENT_USER_ID = 1L;

    private final UserMapper userMapper;

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getCurrentUser() {
        return getById(MOCK_CURRENT_USER_ID);
    }

    @Override
    public User updateCurrentUser(User user) {
        User existingUser = getCurrentUser();
        if (existingUser == null) {
            return null;
        }

        existingUser.setPhone(user.getPhone());
        existingUser.setNickname(user.getNickname());
        existingUser.setAvatar(user.getAvatar());
        existingUser.setGender(user.getGender());
        existingUser.setBirthday(user.getBirthday());
        existingUser.setTastePreference(user.getTastePreference());
        existingUser.setAllergyInfo(user.getAllergyInfo());
        existingUser.setEmergencyContactName(user.getEmergencyContactName());
        existingUser.setEmergencyContactPhone(user.getEmergencyContactPhone());
        existingUser.setUpdatedAt(LocalDateTime.now());

        int rows = userMapper.updateProfileById(existingUser);
        if (rows <= 0) {
            return null;
        }
        return userMapper.selectById(MOCK_CURRENT_USER_ID);
    }
}
