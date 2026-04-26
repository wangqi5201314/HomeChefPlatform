package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.enums.UserStatusEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.AdminStatusUpdateDTO;
import com.homechef.homechefsystem.dto.AdminUserQueryDTO;
import com.homechef.homechefsystem.entity.User;
import com.homechef.homechefsystem.mapper.UserMapper;
import com.homechef.homechefsystem.service.AdminUserService;
import com.homechef.homechefsystem.vo.AdminUserDetailVO;
import com.homechef.homechefsystem.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 后台用户管理服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
     */
    @Override
    public List<AdminUserVO> getUserList(AdminUserQueryDTO queryDTO) {
        List<User> userList = userMapper.selectAdminList(queryDTO);
        if (userList == null || userList.isEmpty()) {
            return Collections.emptyList();
        }
        return userList.stream()
                .map(this::toAdminUserVO)
                .collect(Collectors.toList());
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 后台用户管理服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public AdminUserDetailVO getUserDetail(Long id) {
        return toAdminUserDetailVO(userMapper.selectById(id));
    }

    /**
     * 方法说明：更新一条当前业务场景下的数据记录或状态。
     * 主要作用：该方法用于 后台用户管理服务实现 中的编辑、状态变更或流程推进，保证外部只能修改业务允许变动的部分。
     * 实现逻辑：实现时会先查询原始数据并做归属或状态校验，再回填可编辑字段执行更新，必要时返回更新后的详情结果。
     */
    @Override
    public AdminUserVO updateUserStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO) {
        if (!UserStatusEnum.isValid(statusUpdateDTO.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "user status 取值非法，只能为 0、1");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            return null;
        }

        int rows = userMapper.updateStatusById(id, statusUpdateDTO.getStatus(), LocalDateTime.now());
        if (rows <= 0) {
            return null;
        }

        user.setStatus(statusUpdateDTO.getStatus());
        return toAdminUserVO(user);
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 后台用户管理服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
    private AdminUserVO toAdminUserVO(User user) {
        if (user == null) {
            return null;
        }
        return AdminUserVO.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .tastePreference(user.getTastePreference())
                .status(user.getStatus())
                .statusDesc(UserStatusEnum.getDescByCode(user.getStatus()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 后台用户管理服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
    private AdminUserDetailVO toAdminUserDetailVO(User user) {
        if (user == null) {
            return null;
        }
        return AdminUserDetailVO.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .birthday(user.getBirthday())
                .tastePreference(user.getTastePreference())
                .allergyInfo(user.getAllergyInfo())
                .emergencyContactName(user.getEmergencyContactName())
                .emergencyContactPhone(user.getEmergencyContactPhone())
                .status(user.getStatus())
                .statusDesc(UserStatusEnum.getDescByCode(user.getStatus()))
                .lastLoginTime(user.getLastLoginTime())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
