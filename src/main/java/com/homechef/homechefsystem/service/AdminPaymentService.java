package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.AdminPaymentQueryDTO;
import com.homechef.homechefsystem.vo.AdminPaymentDetailVO;
import com.homechef.homechefsystem.vo.AdminPaymentVO;

import java.util.List;

public interface AdminPaymentService {

    /**
     * 处理 g et pa ym en td et ai lb yo rd er id 相关业务。
     */
    AdminPaymentDetailVO getPaymentDetailByOrderId(Long orderId);

    /**
     * 查询列表数据并返回结果。
     */
    List<AdminPaymentVO> getPaymentList(AdminPaymentQueryDTO queryDTO);
}
