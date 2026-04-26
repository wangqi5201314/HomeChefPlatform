package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.OrderCancelDTO;
import com.homechef.homechefsystem.dto.OrderCreateDTO;
import com.homechef.homechefsystem.dto.OrderQueryDTO;
import com.homechef.homechefsystem.vo.OrderDetailVO;
import com.homechef.homechefsystem.vo.OrderListVO;

import java.util.List;

public interface OrderService {

    /**
     * 创建订单并返回订单详情。
     */
    OrderDetailVO createOrder(OrderCreateDTO orderCreateDTO);

    /**
     * 根据 ID 查询对应数据。
     */
    OrderDetailVO getById(Long id);

    /**
     * 查询列表数据并返回结果。
     */
    List<OrderListVO> getOrderList(OrderQueryDTO queryDTO);

    /**
     * 取消指定订单并返回最新结果。
     */
    OrderDetailVO cancelById(Long id, OrderCancelDTO orderCancelDTO);
}
