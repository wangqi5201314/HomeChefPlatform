package com.homechef.homechefsystem.controller.user;

import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.entity.User;
import com.homechef.homechefsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "user not found");
        }
        return Result.success(user);
    }

    @GetMapping("/me")
    public Result<User> getCurrentUser() {
        User user = userService.getCurrentUser();
        if (user == null) {
            return Result.error(404, "user not found");
        }
        return Result.success(user);
    }

    @PutMapping("/me")
    public Result<User> updateCurrentUser(@RequestBody User user) {
        User updatedUser = userService.updateCurrentUser(user);
        if (updatedUser == null) {
            return Result.error(404, "user not found");
        }
        return Result.success(updatedUser);
    }
}
