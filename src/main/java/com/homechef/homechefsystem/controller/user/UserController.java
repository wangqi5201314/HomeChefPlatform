package com.homechef.homechefsystem.controller.user;

import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.UserUpdateDTO;
import com.homechef.homechefsystem.service.UserService;
import com.homechef.homechefsystem.vo.UserVO;
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
    public Result<UserVO> getById(@PathVariable Long id) {
        UserVO userVO = userService.getById(id);
        if (userVO == null) {
            return Result.error(404, "user not found");
        }
        return Result.success(userVO);
    }

    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        UserVO userVO = userService.getCurrentUser();
        if (userVO == null) {
            return Result.error(404, "user not found");
        }
        return Result.success(userVO);
    }

    @PutMapping("/me")
    public Result<UserVO> updateCurrentUser(@RequestBody UserUpdateDTO userUpdateDTO) {
        UserVO updatedUser = userService.updateCurrentUser(userUpdateDTO);
        if (updatedUser == null) {
            return Result.error(404, "user not found");
        }
        return Result.success(updatedUser);
    }
}
