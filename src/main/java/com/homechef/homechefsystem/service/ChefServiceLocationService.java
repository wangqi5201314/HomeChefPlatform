package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefServiceLocationCreateDTO;
import com.homechef.homechefsystem.dto.ChefServiceLocationUpdateDTO;
import com.homechef.homechefsystem.vo.ChefServiceLocationVO;

import java.util.List;

public interface ChefServiceLocationService {

    List<ChefServiceLocationVO> getCurrentChefServiceLocationList();

    ChefServiceLocationVO getCurrentChefServiceLocationById(Long id);

    ChefServiceLocationVO createCurrentChefServiceLocation(ChefServiceLocationCreateDTO chefServiceLocationCreateDTO);

    ChefServiceLocationVO updateCurrentChefServiceLocation(Long id, ChefServiceLocationUpdateDTO chefServiceLocationUpdateDTO);

    ChefServiceLocationVO deleteCurrentChefServiceLocation(Long id);

    ChefServiceLocationVO activateCurrentChefServiceLocation(Long id);

    List<ChefServiceLocationVO> getChefServiceLocationListByChefId(Long chefId);
}
