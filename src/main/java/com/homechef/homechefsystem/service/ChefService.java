package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefChangePasswordDTO;
import com.homechef.homechefsystem.dto.ChefLoginDTO;
import com.homechef.homechefsystem.dto.ChefQueryDTO;
import com.homechef.homechefsystem.dto.ChefRegisterDTO;
import com.homechef.homechefsystem.dto.ChefUpdateDTO;
import com.homechef.homechefsystem.vo.ChefDetailVO;
import com.homechef.homechefsystem.vo.ChefListVO;
import com.homechef.homechefsystem.vo.ChefVO;

import java.util.List;

public interface ChefService {

    /**
     * 查询列表数据并返回结果。
     */
    List<ChefListVO> getChefList(ChefQueryDTO queryDTO);

    /**
     * 根据 ID 查询对应数据。
     */
    ChefDetailVO getById(Long id);

    /**
     * 根据 ID 更新数据并返回结果。
     */
    ChefDetailVO updateById(Long id, ChefUpdateDTO chefUpdateDTO);

    /**
     * 执行登录并返回登录结果。
     */
    ChefVO login(ChefLoginDTO chefLoginDTO);

    /**
     * 执行注册并返回注册结果。
     */
    ChefVO register(ChefRegisterDTO chefRegisterDTO);

    /**
     * 获取当前登录主体的资料信息。
     */
    ChefVO getCurrentChef();

    /**
     * 更新当前登录主体的资料信息。
     */
    ChefVO updateCurrentChef(ChefUpdateDTO chefUpdateDTO);

    /**
     * 修改当前主体的登录密码。
     */
    void changePassword(ChefChangePasswordDTO chefChangePasswordDTO);
}
