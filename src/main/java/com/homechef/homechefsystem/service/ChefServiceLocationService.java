package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefServiceLocationSaveDTO;
import com.homechef.homechefsystem.vo.ChefServiceLocationVO;

public interface ChefServiceLocationService {

    ChefServiceLocationVO getCurrentChefServiceLocation();

    ChefServiceLocationVO saveCurrentChefServiceLocation(ChefServiceLocationSaveDTO chefServiceLocationSaveDTO);

    ChefServiceLocationVO getByChefId(Long chefId);
}
