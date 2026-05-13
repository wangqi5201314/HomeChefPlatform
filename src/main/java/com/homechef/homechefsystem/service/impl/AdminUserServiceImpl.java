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
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
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
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public AdminUserDetailVO getUserDetail(Long id) {
        return toAdminUserDetailVO(userMapper.selectById(id));
    }

    /**
     * 修改一条已有的业务数据。
     * 这个方法用于更新详情信息、状态或某些可编辑字段。
     * 它会先查出原始数据，再把新值写进去，最后保存并返回更新后的结果。
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
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
