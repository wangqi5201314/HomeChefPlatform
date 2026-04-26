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
     * 方法说明：在 厨师认证服务实现 中处理 submit 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师认证服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public ChefCertificationVO getByChefId(Long chefId) {
        return toChefCertificationVO(chefCertificationMapper.selectByChefId(chefId));
    }

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 厨师认证服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
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
     * 方法说明：在 厨师认证服务实现 中处理 auditById 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：判断指定业务数据是否已经存在。
     * 主要作用：该方法用于 厨师认证服务实现 中的前置去重或存在性验证，避免重复创建或引用无效数据。
     * 实现逻辑：实现逻辑通常会根据主键、业务唯一键或关联条件调用 Mapper 统计结果，再把是否存在返回给上层流程使用。
     */
    @Override
    public boolean chefExists(Long chefId) {
        return chefMapper.selectById(chefId) != null;
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 厨师认证服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public ChefCertificationVO getCurrentChefCertification() {
        return getByChefId(requireCurrentChefId());
    }

    /**
     * 方法说明：在 厨师认证服务实现 中处理 submitCurrentChefCertification 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：获取当前业务必需的数据，并在取不到时立即中断流程。
     * 主要作用：它把 厨师认证服务实现 中“查询 + 非空校验”的重复套路合并成一个辅助方法，让主流程更聚焦业务本身。
     * 实现逻辑：实现时会先根据身份信息或业务键查询目标数据，再补充坐标、状态或归属校验，不满足条件时直接抛出业务异常。
     */
    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 厨师认证服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
