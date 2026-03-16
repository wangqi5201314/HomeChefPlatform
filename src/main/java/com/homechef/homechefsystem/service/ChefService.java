package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefQueryDTO;
import com.homechef.homechefsystem.dto.ChefUpdateDTO;
import com.homechef.homechefsystem.vo.ChefDetailVO;
import com.homechef.homechefsystem.vo.ChefListVO;

import java.util.List;

public interface ChefService {

    List<ChefListVO> getChefList(ChefQueryDTO queryDTO);

    ChefDetailVO getById(Long id);

    ChefDetailVO updateById(Long id, ChefUpdateDTO chefUpdateDTO);
}
