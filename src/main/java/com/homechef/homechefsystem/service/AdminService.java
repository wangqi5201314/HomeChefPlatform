package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.AdminChefQueryDTO;
import com.homechef.homechefsystem.dto.AdminChangePasswordDTO;
import com.homechef.homechefsystem.dto.AdminLoginDTO;
import com.homechef.homechefsystem.dto.AdminOrderQueryDTO;
import com.homechef.homechefsystem.dto.AdminStatusUpdateDTO;
import com.homechef.homechefsystem.dto.AdminUserQueryDTO;
import com.homechef.homechefsystem.vo.AdminChefVO;
import com.homechef.homechefsystem.vo.AdminLoginVO;
import com.homechef.homechefsystem.vo.AdminOrderVO;
import com.homechef.homechefsystem.vo.AdminUserVO;
import com.homechef.homechefsystem.vo.OrderDetailVO;

import java.util.List;

public interface AdminService {

    AdminLoginVO login(AdminLoginDTO adminLoginDTO);

    void changePassword(AdminChangePasswordDTO adminChangePasswordDTO);

    List<AdminUserVO> getUserList(AdminUserQueryDTO queryDTO);

    AdminUserVO updateUserStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO);

    List<AdminChefVO> getChefList(AdminChefQueryDTO queryDTO);

    AdminChefVO updateChefStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO);

    List<AdminOrderVO> getOrderList(AdminOrderQueryDTO queryDTO);

    OrderDetailVO getOrderDetail(Long id);
}
