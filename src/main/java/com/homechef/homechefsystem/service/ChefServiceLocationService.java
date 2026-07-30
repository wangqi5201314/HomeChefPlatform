package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefServiceLocationCreateDTO;
import com.homechef.homechefsystem.dto.ChefServiceLocationUpdateDTO;
import com.homechef.homechefsystem.vo.ChefServiceLocationVO;

import java.util.List;

public interface ChefServiceLocationService {

    /**
     * 获取当前登录厨师的服务位置列表。
     */
    List<ChefServiceLocationVO> getCurrentChefServiceLocationList();

    /**
     * 获取当前登录厨师名下的服务位置详情。
     */
    ChefServiceLocationVO getCurrentChefServiceLocationById(Long id);

    /**
     * 为当前登录厨师新增服务位置。
     */
    ChefServiceLocationVO createCurrentChefServiceLocation(ChefServiceLocationCreateDTO chefServiceLocationCreateDTO);

    /**
     * 更新当前登录厨师名下的服务位置。
     */
    ChefServiceLocationVO updateCurrentChefServiceLocation(Long id, ChefServiceLocationUpdateDTO chefServiceLocationUpdateDTO);

    /**
     * 删除当前登录厨师名下的服务位置。
     */
    ChefServiceLocationVO deleteCurrentChefServiceLocation(Long id);

    /**
     * 启用当前登录厨师指定的服务位置。
     */
    ChefServiceLocationVO activateCurrentChefServiceLocation(Long id);

    /**
     * 获取指定厨师的服务位置列表。
     */
    List<ChefServiceLocationVO> getChefServiceLocationListByChefId(Long chefId);
}
