package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.AdminOrderQueryDTO;
import com.homechef.homechefsystem.vo.AdminOrderVO;

import java.util.List;

public interface AdminOrderService {

    List<AdminOrderVO> getOrderList(AdminOrderQueryDTO queryDTO);
}
