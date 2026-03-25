package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.AdminPaymentQueryDTO;
import com.homechef.homechefsystem.vo.AdminPaymentDetailVO;
import com.homechef.homechefsystem.vo.AdminPaymentVO;

import java.util.List;

public interface AdminPaymentService {

    AdminPaymentDetailVO getPaymentDetailByOrderId(Long orderId);

    List<AdminPaymentVO> getPaymentList(AdminPaymentQueryDTO queryDTO);
}
