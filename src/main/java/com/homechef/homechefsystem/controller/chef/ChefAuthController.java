package com.homechef.homechefsystem.controller.chef;

import com.homechef.homechefsystem.annotation.RequireLogin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.ChefChangePasswordDTO;
import com.homechef.homechefsystem.dto.ChefLoginDTO;
import com.homechef.homechefsystem.dto.ChefUpdateDTO;
import com.homechef.homechefsystem.dto.LoginTokenDTO;
import com.homechef.homechefsystem.service.ChefService;
import com.homechef.homechefsystem.utils.JwtUtil;
import com.homechef.homechefsystem.vo.ChefVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chef")
@RequiredArgsConstructor
@Tag(name = "厨师认证接口")
public class ChefAuthController {

    private final ChefService chefService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "厨师登录")
    @PostMapping("/login")
    public Result<LoginTokenDTO> login(@Valid @RequestBody ChefLoginDTO chefLoginDTO) {
        ChefVO chefVO = chefService.login(chefLoginDTO);
        return Result.success(LoginTokenDTO.builder()
                .token(jwtUtil.generateChefToken(chefVO.getId()))
                .userType(JwtUtil.USER_TYPE_CHEF)
                .userId(0L)
                .adminId(0L)
                .chefId(chefVO.getId())
                .build());
    }

    @RequireLogin
    @Operation(summary = "获取当前登录厨师信息")
    @GetMapping("/me")
    public Result<ChefVO> getCurrentChef() {
        ChefVO chefVO = chefService.getCurrentChef();
        if (chefVO == null) {
            return Result.error(404, "chef not found");
        }
        return Result.success(chefVO);
    }

    @RequireLogin
    @Operation(summary = "修改当前登录厨师信息")
    @PutMapping("/me")
    public Result<ChefVO> updateCurrentChef(@RequestBody ChefUpdateDTO chefUpdateDTO) {
        ChefVO chefVO = chefService.updateCurrentChef(chefUpdateDTO);
        if (chefVO == null) {
            return Result.error(404, "chef not found");
        }
        return Result.success(chefVO);
    }

    @RequireLogin
    @Operation(summary = "修改厨师密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChefChangePasswordDTO chefChangePasswordDTO) {
        chefService.changePassword(chefChangePasswordDTO);
        return Result.success();
    }
}
