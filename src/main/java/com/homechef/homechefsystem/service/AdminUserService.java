package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.AdminStatusUpdateDTO;
import com.homechef.homechefsystem.dto.AdminUserQueryDTO;
import com.homechef.homechefsystem.vo.AdminUserDetailVO;
import com.homechef.homechefsystem.vo.AdminUserVO;

import java.util.List;

public interface AdminUserService {

    /**
     * 查询列表数据并返回结果。
     */
    List<AdminUserVO> getUserList(AdminUserQueryDTO queryDTO);

    /**
     * 查询详情数据并返回结果。
     */
    AdminUserDetailVO getUserDetail(Long id);

    /**
     * 处理 u pd at eu se rs ta tu s 相关业务。
     */
    AdminUserVO updateUserStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO);
}
