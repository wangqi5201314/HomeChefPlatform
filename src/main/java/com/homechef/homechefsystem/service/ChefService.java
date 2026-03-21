package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefChangePasswordDTO;
import com.homechef.homechefsystem.dto.ChefLoginDTO;
import com.homechef.homechefsystem.dto.ChefQueryDTO;
import com.homechef.homechefsystem.dto.ChefUpdateDTO;
import com.homechef.homechefsystem.vo.ChefDetailVO;
import com.homechef.homechefsystem.vo.ChefListVO;
import com.homechef.homechefsystem.vo.ChefVO;

import java.util.List;

public interface ChefService {

    List<ChefListVO> getChefList(ChefQueryDTO queryDTO);

    ChefDetailVO getById(Long id);

    ChefDetailVO updateById(Long id, ChefUpdateDTO chefUpdateDTO);

    ChefVO login(ChefLoginDTO chefLoginDTO);

    ChefVO getCurrentChef();

    ChefVO updateCurrentChef(ChefUpdateDTO chefUpdateDTO);

    void changePassword(ChefChangePasswordDTO chefChangePasswordDTO);
}
