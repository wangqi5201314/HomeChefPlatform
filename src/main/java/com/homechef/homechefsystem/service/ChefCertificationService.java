package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefCertificationAuditDTO;
import com.homechef.homechefsystem.dto.ChefCertificationQueryDTO;
import com.homechef.homechefsystem.dto.ChefCertificationSubmitDTO;
import com.homechef.homechefsystem.vo.ChefCertificationVO;

import java.util.List;

public interface ChefCertificationService {

    /**
     * 提交业务数据并返回处理结果。
     */
    ChefCertificationVO submit(ChefCertificationSubmitDTO chefCertificationSubmitDTO);

    /**
     * 根据厨师 ID 查询对应数据。
     */
    ChefCertificationVO getByChefId(Long chefId);

    /**
     * 处理 g et li st 相关业务。
     */
    List<ChefCertificationVO> getList(ChefCertificationQueryDTO queryDTO);

    /**
     * 根据 ID 审核指定记录。
     */
    ChefCertificationVO auditById(Long id, ChefCertificationAuditDTO chefCertificationAuditDTO);

    /**
     * 判断指定厨师是否存在。
     */
    boolean chefExists(Long chefId);

    /**
     * 获取当前登录厨师的认证信息。
     */
    ChefCertificationVO getCurrentChefCertification();

    /**
     * 提交当前登录厨师的认证资料。
     */
    ChefCertificationVO submitCurrentChefCertification(ChefCertificationSubmitDTO chefCertificationSubmitDTO);
}
