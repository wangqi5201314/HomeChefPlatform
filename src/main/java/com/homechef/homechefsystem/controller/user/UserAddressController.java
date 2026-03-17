package com.homechef.homechefsystem.controller.user;

import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.UserAddressCreateDTO;
import com.homechef.homechefsystem.dto.UserAddressQueryDTO;
import com.homechef.homechefsystem.dto.UserAddressUpdateDTO;
import com.homechef.homechefsystem.service.UserAddressService;
import com.homechef.homechefsystem.vo.UserAddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/address")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @GetMapping("/list")
    public Result<List<UserAddressVO>> getAddressList(UserAddressQueryDTO queryDTO) {
        return Result.success(userAddressService.getAddressList(queryDTO));
    }

    @GetMapping("/default")
    public Result<UserAddressVO> getDefaultAddress(UserAddressQueryDTO queryDTO) {
        UserAddressVO userAddressVO = userAddressService.getDefaultAddress(queryDTO.getUserId());
        if (userAddressVO == null) {
            return Result.error(404, "address not found");
        }
        return Result.success(userAddressVO);
    }

    @GetMapping("/{id}")
    public Result<UserAddressVO> getById(@PathVariable Long id) {
        UserAddressVO userAddressVO = userAddressService.getById(id);
        if (userAddressVO == null) {
            return Result.error(404, "address not found");
        }
        return Result.success(userAddressVO);
    }

    @PostMapping
    public Result<UserAddressVO> create(@RequestBody UserAddressCreateDTO userAddressCreateDTO) {
        UserAddressVO userAddressVO = userAddressService.create(userAddressCreateDTO);
        if (userAddressVO == null) {
            return Result.error(500, "create address failed");
        }
        return Result.success(userAddressVO);
    }

    @PutMapping("/{id}")
    public Result<UserAddressVO> updateById(@PathVariable Long id,
                                            @RequestBody UserAddressUpdateDTO userAddressUpdateDTO) {
        UserAddressVO userAddressVO = userAddressService.updateById(id, userAddressUpdateDTO);
        if (userAddressVO == null) {
            return Result.error(404, "address not found");
        }
        return Result.success(userAddressVO);
    }

    @PostMapping("/{id}/default")
    public Result<UserAddressVO> setDefaultById(@PathVariable Long id, @RequestBody UserAddressQueryDTO queryDTO) {
        UserAddressVO userAddressVO = userAddressService.setDefaultById(id, queryDTO.getUserId());
        if (userAddressVO == null) {
            return Result.error(404, "address not found");
        }
        return Result.success(userAddressVO);
    }

    @DeleteMapping("/{id}")
    public Result<UserAddressVO> deleteById(@PathVariable Long id) {
        UserAddressVO userAddressVO = userAddressService.deleteById(id);
        if (userAddressVO == null) {
            return Result.error(404, "address not found");
        }
        return Result.success(userAddressVO);
    }
}
