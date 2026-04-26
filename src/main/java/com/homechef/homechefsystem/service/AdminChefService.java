package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.AdminStatusUpdateDTO;
import com.homechef.homechefsystem.vo.AdminChefDetailVO;

public interface AdminChefService {

    /**
     * 处理 u pd at ec he fs ta tu s 相关业务。
     */
    void updateChefStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO);

    /**
     * 查询详情数据并返回结果。
     */
    AdminChefDetailVO getChefDetail(Long id);
}
