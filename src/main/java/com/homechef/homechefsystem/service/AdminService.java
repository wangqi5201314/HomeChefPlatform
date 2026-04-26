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

    /**
     * 执行登录并返回登录结果。
     */
    AdminLoginVO login(AdminLoginDTO adminLoginDTO);

    /**
     * 修改当前主体的登录密码。
     */
    void changePassword(AdminChangePasswordDTO adminChangePasswordDTO);

    /**
     * 查询列表数据并返回结果。
     */
    List<AdminUserVO> getUserList(AdminUserQueryDTO queryDTO);

    /**
     * 处理 u pd at eu se rs ta tu s 相关业务。
     */
    AdminUserVO updateUserStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO);

    /**
     * 查询列表数据并返回结果。
     */
    List<AdminChefVO> getChefList(AdminChefQueryDTO queryDTO);

    /**
     * 处理 u pd at ec he fs ta tu s 相关业务。
     */
    AdminChefVO updateChefStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO);

    /**
     * 查询列表数据并返回结果。
     */
    List<AdminOrderVO> getOrderList(AdminOrderQueryDTO queryDTO);

    /**
     * 查询详情数据并返回结果。
     */
    OrderDetailVO getOrderDetail(Long id);
}
