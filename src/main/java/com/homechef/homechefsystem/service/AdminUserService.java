package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.AdminStatusUpdateDTO;
import com.homechef.homechefsystem.dto.AdminUserQueryDTO;
import com.homechef.homechefsystem.vo.AdminUserDetailVO;
import com.homechef.homechefsystem.vo.AdminUserVO;

import java.util.List;

public interface AdminUserService {

    List<AdminUserVO> getUserList(AdminUserQueryDTO queryDTO);

    AdminUserDetailVO getUserDetail(Long id);

    AdminUserVO updateUserStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO);
}
