package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ChefCertStatusEnum;
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

    /**
     * 处理 submit 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
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
                    .auditStatus(ChefCertStatusEnum.PENDING.getCode())
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
            existingCertification.setAuditStatus(ChefCertStatusEnum.PENDING.getCode());
            existingCertification.setAuditRemark(null);
            existingCertification.setSubmittedAt(now);
            existingCertification.setAuditedAt(null);

            int rows = chefCertificationMapper.updateByChefId(existingCertification);
            if (rows <= 0) {
                return null;
            }
        }

        chefMapper.updateCertStatusById(
                chefCertificationSubmitDTO.getChefId(),
                ChefCertStatusEnum.PENDING.getCode(),
                now
        );
        return toChefCertificationVO(chefCertificationMapper.selectByChefId(chefCertificationSubmitDTO.getChefId()));
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public ChefCertificationVO getByChefId(Long chefId) {
        return toChefCertificationVO(chefCertificationMapper.selectByChefId(chefId));
    }

    /**
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
     */
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

    /**
     * 处理 auditById 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
    @Override
    public ChefCertificationVO auditById(Long id, ChefCertificationAuditDTO chefCertificationAuditDTO) {
        ChefCertification existingCertification = chefCertificationMapper.selectById(id);
        if (existingCertification == null) {
            return null;
        }
        if (!ChefCertStatusEnum.isAuditResult(chefCertificationAuditDTO.getAuditStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "审核状态取值非法，只能为 1、2");
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

    /**
     * 判断某条数据是否已经存在。
     * 这个方法主要用于创建前去重，或者先判断关联数据是不是有效。
     * 它会按给定条件去查数据库，然后把是否存在的结果返回出来。
     */
    @Override
    public boolean chefExists(Long chefId) {
        return chefMapper.selectById(chefId) != null;
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public ChefCertificationVO getCurrentChefCertification() {
        return getByChefId(requireCurrentChefId());
    }

    /**
     * 处理 submitCurrentChefCertification 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
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

    /**
     * 查出当前业务必须要用的数据。
     * 这个方法用于把“先查数据，找不到就报错”这类逻辑集中到一起。
     * 它会根据 id 或当前登录信息去查记录，如果查不到或不符合条件，就直接抛出异常。
     */
    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    /**
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
     */
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
