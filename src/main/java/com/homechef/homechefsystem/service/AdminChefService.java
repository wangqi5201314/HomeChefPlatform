package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.AdminStatusUpdateDTO;
import com.homechef.homechefsystem.vo.AdminChefDetailVO;

public interface AdminChefService {

    void updateChefStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO);

    AdminChefDetailVO getChefDetail(Long id);
}
