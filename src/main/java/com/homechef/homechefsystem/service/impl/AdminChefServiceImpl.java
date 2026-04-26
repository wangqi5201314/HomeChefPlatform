package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ChefCertStatusEnum;
import com.homechef.homechefsystem.common.enums.ChefServiceModeEnum;
import com.homechef.homechefsystem.common.enums.ChefStatusEnum;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.AdminStatusUpdateDTO;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.service.AdminChefService;
import com.homechef.homechefsystem.vo.AdminChefDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminChefServiceImpl implements AdminChefService {

    private final ChefMapper chefMapper;

    /**
     * 方法说明：更新一条当前业务场景下的数据记录或状态。
     * 主要作用：该方法用于 后台厨师管理服务实现 中的编辑、状态变更或流程推进，保证外部只能修改业务允许变动的部分。
     * 实现逻辑：实现时会先查询原始数据并做归属或状态校验，再回填可编辑字段执行更新，必要时返回更新后的详情结果。
     */
    @Override
    public void updateChefStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO) {
        if (!ChefStatusEnum.isValid(statusUpdateDTO.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "chef status 取值非法，只能为 0、1");
        }

        Chef chef = chefMapper.selectById(id);
        if (chef == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "chef not found");
        }

        int rows = chefMapper.updateStatusById(id, statusUpdateDTO.getStatus(), LocalDateTime.now());
        if (rows <= 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "update chef status failed");
        }
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 后台厨师管理服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public AdminChefDetailVO getChefDetail(Long id) {
        return toAdminChefDetailVO(chefMapper.selectById(id));
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 后台厨师管理服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
    private AdminChefDetailVO toAdminChefDetailVO(Chef chef) {
        if (chef == null) {
            return null;
        }
        return AdminChefDetailVO.builder()
                .id(chef.getId())
                .name(chef.getName())
                .phone(chef.getPhone())
                .avatar(chef.getAvatar())
                .gender(chef.getGender())
                .age(chef.getAge())
                .introduction(chef.getIntroduction())
                .specialtyCuisine(chef.getSpecialtyCuisine())
                .specialtyTags(chef.getSpecialtyTags())
                .yearsOfExperience(chef.getYearsOfExperience())
                .serviceRadiusKm(chef.getServiceRadiusKm())
                .serviceMode(chef.getServiceMode())
                .serviceModeDesc(ChefServiceModeEnum.getDescByCode(chef.getServiceMode()))
                .ratingAvg(chef.getRatingAvg())
                .orderCount(chef.getOrderCount())
                .onTimeRate(chef.getOnTimeRate())
                .goodReviewRate(chef.getGoodReviewRate())
                .certStatus(chef.getCertStatus())
                .certStatusDesc(ChefCertStatusEnum.getDescByCode(chef.getCertStatus()))
                .status(chef.getStatus())
                .statusDesc(ChefStatusEnum.getDescByCode(chef.getStatus()))
                .createdAt(chef.getCreatedAt())
                .updatedAt(chef.getUpdatedAt())
                .build();
    }
}
