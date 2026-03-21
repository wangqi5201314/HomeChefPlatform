package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefOrderRejectDTO;
import com.homechef.homechefsystem.vo.ChefOrderDetailVO;
import com.homechef.homechefsystem.vo.ChefOrderListVO;

import java.util.List;

public interface ChefOrderService {

    List<ChefOrderListVO> getCurrentChefOrderList(String orderStatus);

    ChefOrderDetailVO getCurrentChefOrderDetail(Long id);

    ChefOrderDetailVO accept(Long id);

    ChefOrderDetailVO reject(Long id, ChefOrderRejectDTO chefOrderRejectDTO);

    ChefOrderDetailVO start(Long id);

    ChefOrderDetailVO finish(Long id);
}
