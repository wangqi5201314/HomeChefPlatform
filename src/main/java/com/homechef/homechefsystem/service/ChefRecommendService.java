package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefRecommendQueryDTO;
import com.homechef.homechefsystem.vo.ChefRecommendVO;

import java.util.List;

public interface ChefRecommendService {

    /**
     * 按条件获取厨师推荐列表。
     */
    List<ChefRecommendVO> recommend(ChefRecommendQueryDTO chefRecommendQueryDTO);

    /**
     * 获取首页默认推荐的厨师列表。
     */
    List<ChefRecommendVO> recommendDefault(Long userId, Long addressId);
}
