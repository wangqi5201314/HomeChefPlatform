package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefRecommendQueryDTO;
import com.homechef.homechefsystem.vo.ChefRecommendVO;

import java.util.List;

public interface ChefRecommendService {

    List<ChefRecommendVO> recommend(ChefRecommendQueryDTO chefRecommendQueryDTO);

    List<ChefRecommendVO> recommendDefault(Long userId, Long addressId);
}
