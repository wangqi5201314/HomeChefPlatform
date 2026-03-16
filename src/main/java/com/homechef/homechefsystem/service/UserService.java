package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.entity.User;

public interface UserService {

    User getById(Long id);

    User getCurrentUser();

    User updateCurrentUser(User user);
}
