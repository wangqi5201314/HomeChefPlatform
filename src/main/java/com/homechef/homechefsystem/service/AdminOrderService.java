package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.AdminOrderQueryDTO;
import com.homechef.homechefsystem.vo.AdminOrderVO;

import java.util.List;

public interface AdminOrderService {

    /**
     * 查询列表数据并返回结果。
     */
    List<AdminOrderVO> getOrderList(AdminOrderQueryDTO queryDTO);
}
