package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.ChefCertificationAuditDTO;
import com.homechef.homechefsystem.dto.ChefCertificationQueryDTO;
import com.homechef.homechefsystem.dto.ChefCertificationSubmitDTO;
import com.homechef.homechefsystem.entity.ChefCertification;
import com.homechef.homechefsystem.mapper.ChefCertificationMapper;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.service.ChefCertificationService;
import com.homechef.homechefsystem.utils.LoginUserContext;
import com.homechef.homechefsystem.vo.ChefCertificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChefCertificationServiceImpl implements ChefCertificationService {

    private final ChefCertificationMapper chefCertificationMapper;

    private final ChefMapper chefMapper;

    @Override
    public ChefCertificationVO submit(ChefCertificationSubmitDTO chefCertificationSubmitDTO) {
        LocalDateTime now = LocalDateTime.now();
        ChefCertification existingCertification = chefCertificationMapper.selectByChefId(chefCertificationSubmitDTO.getChefId());

        if (existingCertification == null) {
            ChefCertification chefCertification = ChefCertification.builder()
                    .chefId(chefCertificationSubmitDTO.getChefId())
                    .realName(chefCertificationSubmitDTO.getRealName())
                    .idCardNo(chefCertificationSubmitDTO.getIdCardNo())
                    .healthCertUrl(chefCertificationSubmitDTO.getHealthCertUrl())
                    .skillCertUrl(chefCertificationSubmitDTO.getSkillCertUrl())
                    .serviceCertUrl(chefCertificationSubmitDTO.getServiceCertUrl())
                    .advancedCertUrl(chefCertificationSubmitDTO.getAdvancedCertUrl())
                    .auditStatus(0)
                    .auditRemark(null)
                    .submittedAt(now)
                    .auditedAt(null)
                    .build();

            int rows = chefCertificationMapper.insert(chefCertification);
            if (rows <= 0) {
                return null;
            }
        } else {
            existingCertification.setRealName(chefCertificationSubmitDTO.getRealName());
            existingCertification.setIdCardNo(chefCertificationSubmitDTO.getIdCardNo());
            existingCertification.setHealthCertUrl(chefCertificationSubmitDTO.getHealthCertUrl());
            existingCertification.setSkillCertUrl(chefCertificationSubmitDTO.getSkillCertUrl());
            existingCertification.setServiceCertUrl(chefCertificationSubmitDTO.getServiceCertUrl());
            existingCertification.setAdvancedCertUrl(chefCertificationSubmitDTO.getAdvancedCertUrl());
            existingCertification.setAuditStatus(0);
            existingCertification.setAuditRemark(null);
            existingCertification.setSubmittedAt(now);
            existingCertification.setAuditedAt(null);

            int rows = chefCertificationMapper.updateByChefId(existingCertification);
            if (rows <= 0) {
                return null;
            }
        }

        chefMapper.updateCertStatusById(chefCertificationSubmitDTO.getChefId(), 0, now);
        return toChefCertificationVO(chefCertificationMapper.selectByChefId(chefCertificationSubmitDTO.getChefId()));
    }

    @Override
    public ChefCertificationVO getByChefId(Long chefId) {
        return toChefCertificationVO(chefCertificationMapper.selectByChefId(chefId));
    }

    @Override
    public List<ChefCertificationVO> getList(ChefCertificationQueryDTO queryDTO) {
        List<ChefCertification> certificationList = chefCertificationMapper.selectList(queryDTO);
        if (certificationList == null || certificationList.isEmpty()) {
            return Collections.emptyList();
        }
        return certificationList.stream()
                .map(this::toChefCertificationVO)
                .collect(Collectors.toList());
    }

    @Override
    public ChefCertificationVO auditById(Long id, ChefCertificationAuditDTO chefCertificationAuditDTO) {
        ChefCertification existingCertification = chefCertificationMapper.selectById(id);
        if (existingCertification == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        int rows = chefCertificationMapper.updateAuditById(
                id,
                chefCertificationAuditDTO.getAuditStatus(),
                chefCertificationAuditDTO.getAuditRemark(),
                now
        );
        if (rows <= 0) {
            return null;
        }

        chefMapper.updateCertStatusById(existingCertification.getChefId(), chefCertificationAuditDTO.getAuditStatus(), now);
        return toChefCertificationVO(chefCertificationMapper.selectById(id));
    }

    @Override
    public boolean chefExists(Long chefId) {
        return chefMapper.selectById(chefId) != null;
    }

    @Override
    public ChefCertificationVO getCurrentChefCertification() {
        return getByChefId(requireCurrentChefId());
    }

    @Override
    public ChefCertificationVO submitCurrentChefCertification(ChefCertificationSubmitDTO chefCertificationSubmitDTO) {
        Long chefId = requireCurrentChefId();
        chefCertificationSubmitDTO.setChefId(chefId);
        if (!chefExists(chefId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "chef not found");
        }
        ChefCertificationVO chefCertificationVO = submit(chefCertificationSubmitDTO);
        if (chefCertificationVO == null) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "submit certification failed");
        }
        return chefCertificationVO;
    }

    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    private ChefCertificationVO toChefCertificationVO(ChefCertification chefCertification) {
        if (chefCertification == null) {
            return null;
        }
        return ChefCertificationVO.builder()
                .id(chefCertification.getId())
                .chefId(chefCertification.getChefId())
                .realName(chefCertification.getRealName())
                .idCardNo(chefCertification.getIdCardNo())
                .healthCertUrl(chefCertification.getHealthCertUrl())
                .skillCertUrl(chefCertification.getSkillCertUrl())
                .serviceCertUrl(chefCertification.getServiceCertUrl())
                .advancedCertUrl(chefCertification.getAdvancedCertUrl())
                .auditStatus(chefCertification.getAuditStatus())
                .auditRemark(chefCertification.getAuditRemark())
                .submittedAt(chefCertification.getSubmittedAt())
                .auditedAt(chefCertification.getAuditedAt())
                .build();
    }
}
