package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefOrderRejectDTO;
import com.homechef.homechefsystem.vo.ChefOrderDetailVO;
import com.homechef.homechefsystem.vo.ChefOrderListVO;

import java.util.List;

public interface ChefOrderService {

    /**
     * 获取当前登录厨师的订单列表。
     */
    List<ChefOrderListVO> getCurrentChefOrderList(String orderStatus);

    /**
     * 获取当前登录厨师可查看的订单详情。
     */
    ChefOrderDetailVO getCurrentChefOrderDetail(Long id);

    /**
     * 执行接单处理。
     */
    ChefOrderDetailVO accept(Long id);

    /**
     * 执行拒单处理。
     */
    ChefOrderDetailVO reject(Long id, ChefOrderRejectDTO chefOrderRejectDTO);

    /**
     * 执行开始服务处理。
     */
    ChefOrderDetailVO start(Long id);

    /**
     * 执行完成服务处理。
     */
    ChefOrderDetailVO finish(Long id);
}
