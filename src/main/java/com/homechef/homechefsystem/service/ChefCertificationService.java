package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ChefCertificationAuditDTO;
import com.homechef.homechefsystem.dto.ChefCertificationQueryDTO;
import com.homechef.homechefsystem.dto.ChefCertificationSubmitDTO;
import com.homechef.homechefsystem.vo.ChefCertificationVO;

import java.util.List;

public interface ChefCertificationService {

    ChefCertificationVO submit(ChefCertificationSubmitDTO chefCertificationSubmitDTO);

    ChefCertificationVO getByChefId(Long chefId);

    List<ChefCertificationVO> getList(ChefCertificationQueryDTO queryDTO);

    ChefCertificationVO auditById(Long id, ChefCertificationAuditDTO chefCertificationAuditDTO);

    boolean chefExists(Long chefId);

    ChefCertificationVO getCurrentChefCertification();

    ChefCertificationVO submitCurrentChefCertification(ChefCertificationSubmitDTO chefCertificationSubmitDTO);
}
