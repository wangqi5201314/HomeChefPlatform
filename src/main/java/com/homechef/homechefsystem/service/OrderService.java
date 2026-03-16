package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.OrderCancelDTO;
import com.homechef.homechefsystem.dto.OrderCreateDTO;
import com.homechef.homechefsystem.dto.OrderQueryDTO;
import com.homechef.homechefsystem.vo.OrderDetailVO;
import com.homechef.homechefsystem.vo.OrderListVO;

import java.util.List;

public interface OrderService {

    OrderDetailVO createOrder(OrderCreateDTO orderCreateDTO);

    OrderDetailVO getById(Long id);

    List<OrderListVO> getOrderList(OrderQueryDTO queryDTO);

    OrderDetailVO cancelById(Long id, OrderCancelDTO orderCancelDTO);
}
