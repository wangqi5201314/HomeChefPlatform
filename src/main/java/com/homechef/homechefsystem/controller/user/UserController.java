package com.homechef.homechefsystem.controller.user;

import com.homechef.homechefsystem.annotation.RequireLogin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.LoginTokenDTO;
import com.homechef.homechefsystem.dto.UserLoginDTO;
import com.homechef.homechefsystem.dto.UserUpdateDTO;
import com.homechef.homechefsystem.service.UserService;
import com.homechef.homechefsystem.utils.JwtUtil;
import com.homechef.homechefsystem.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户接口")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginTokenDTO> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        UserVO userVO = userService.login(userLoginDTO);
        if (userVO == null) {
            return Result.error(401, "user not found");
        }

        return Result.success(LoginTokenDTO.builder()
                .token(jwtUtil.generateUserToken(userVO.getId()))
                .userType(JwtUtil.USER_TYPE_USER)
                .userId(userVO.getId())
                .build());
    }

    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        UserVO userVO = userService.getById(id);
        if (userVO == null) {
            return Result.error(404, "user not found");
        }
        return Result.success(userVO);
    }

    @RequireLogin
    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        UserVO userVO = userService.getCurrentUser();
        if (userVO == null) {
            return Result.error(404, "user not found");
        }
        return Result.success(userVO);
    }

    @RequireLogin
    @Operation(summary = "修改当前登录用户信息")
    @PutMapping("/me")
    public Result<UserVO> updateCurrentUser(@RequestBody UserUpdateDTO userUpdateDTO) {
        UserVO updatedUser = userService.updateCurrentUser(userUpdateDTO);
        if (updatedUser == null) {
            return Result.error(404, "user not found");
        }
        return Result.success(updatedUser);
    }
}
