package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ChefCertStatusEnum;
import com.homechef.homechefsystem.common.enums.ChefStatusEnum;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.enums.TimeSlotEnum;
import com.homechef.homechefsystem.common.enums.UserStatusEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.AdminChefQueryDTO;
import com.homechef.homechefsystem.dto.AdminChangePasswordDTO;
import com.homechef.homechefsystem.dto.AdminLoginDTO;
import com.homechef.homechefsystem.dto.AdminOrderQueryDTO;
import com.homechef.homechefsystem.dto.AdminStatusUpdateDTO;
import com.homechef.homechefsystem.dto.AdminUserQueryDTO;
import com.homechef.homechefsystem.entity.Admin;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.entity.Order;
import com.homechef.homechefsystem.entity.User;
import com.homechef.homechefsystem.mapper.AdminMapper;
import com.homechef.homechefsystem.mapper.OrderMapper;
import com.homechef.homechefsystem.service.AdminService;
import com.homechef.homechefsystem.utils.LoginUserContext;
import com.homechef.homechefsystem.vo.AdminChefVO;
import com.homechef.homechefsystem.vo.AdminLoginVO;
import com.homechef.homechefsystem.vo.AdminOrderVO;
import com.homechef.homechefsystem.vo.AdminUserVO;
import com.homechef.homechefsystem.vo.OrderDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;

    private final OrderMapper orderMapper;

    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 方法说明：在 后台综合管理服务实现 中处理 login 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
    public AdminLoginVO login(AdminLoginDTO adminLoginDTO) {
        Admin admin = adminMapper.selectByUsername(adminLoginDTO.getUsername());
        if (admin == null) {
            return null;
        }
        if (!passwordMatches(adminLoginDTO.getPassword(), admin.getPassword())) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        adminMapper.updateLoginTimeById(admin.getId(), now, now);
        return toAdminLoginVO(adminMapper.selectByUsername(adminLoginDTO.getUsername()));
    }

    /**
     * 方法说明：在 后台综合管理服务实现 中处理 changePassword 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    @Override
    public void changePassword(AdminChangePasswordDTO adminChangePasswordDTO) {
        Long currentAdminId = LoginUserContext.getAdminId();
        if (currentAdminId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        if (!adminChangePasswordDTO.getNewPassword().equals(adminChangePasswordDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "confirmPassword does not match newPassword");
        }

        Admin admin = adminMapper.selectById(currentAdminId);
        if (admin == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "admin not found");
        }
        if (!StringUtils.hasText(admin.getPassword())) {
            throw new BusinessException(ResultCodeEnum.FAIL, "password is not set");
        }
        if (!passwordMatches(adminChangePasswordDTO.getOldPassword(), admin.getPassword())) {
            throw new BusinessException(ResultCodeEnum.FAIL, "old password is incorrect");
        }

        int rows = adminMapper.updatePasswordById(
                currentAdminId,
                passwordEncoder.encode(adminChangePasswordDTO.getNewPassword()),
                LocalDateTime.now()
        );
        if (rows <= 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "change password failed");
        }
    }

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 后台综合管理服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
     */
    @Override
    public List<AdminUserVO> getUserList(AdminUserQueryDTO queryDTO) {
        List<User> userList = adminMapper.selectUserList(queryDTO);
        if (userList == null || userList.isEmpty()) {
            return Collections.emptyList();
        }
        return userList.stream()
                .map(this::toAdminUserVO)
                .collect(Collectors.toList());
    }

    /**
     * 方法说明：更新一条当前业务场景下的数据记录或状态。
     * 主要作用：该方法用于 后台综合管理服务实现 中的编辑、状态变更或流程推进，保证外部只能修改业务允许变动的部分。
     * 实现逻辑：实现时会先查询原始数据并做归属或状态校验，再回填可编辑字段执行更新，必要时返回更新后的详情结果。
     */
    @Override
    public AdminUserVO updateUserStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO) {
        if (!UserStatusEnum.isValid(statusUpdateDTO.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "user status 取值非法，只能为 0、1");
        }

        List<User> userList = adminMapper.selectUserList(AdminUserQueryDTO.builder().build());
        User targetUser = userList.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (targetUser == null) {
            return null;
        }

        int rows = adminMapper.updateUserStatusById(id, statusUpdateDTO.getStatus(), LocalDateTime.now());
        if (rows <= 0) {
            return null;
        }

        targetUser.setStatus(statusUpdateDTO.getStatus());
        return toAdminUserVO(targetUser);
    }

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 后台综合管理服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
     */
    @Override
    public List<AdminChefVO> getChefList(AdminChefQueryDTO queryDTO) {
        List<Chef> chefList = adminMapper.selectChefList(queryDTO);
        if (chefList == null || chefList.isEmpty()) {
            return Collections.emptyList();
        }
        return chefList.stream()
                .map(this::toAdminChefVO)
                .collect(Collectors.toList());
    }

    /**
     * 方法说明：更新一条当前业务场景下的数据记录或状态。
     * 主要作用：该方法用于 后台综合管理服务实现 中的编辑、状态变更或流程推进，保证外部只能修改业务允许变动的部分。
     * 实现逻辑：实现时会先查询原始数据并做归属或状态校验，再回填可编辑字段执行更新，必要时返回更新后的详情结果。
     */
    @Override
    public AdminChefVO updateChefStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO) {
        if (!ChefStatusEnum.isValid(statusUpdateDTO.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "chef status 取值非法，只能为 0、1");
        }

        List<Chef> chefList = adminMapper.selectChefList(AdminChefQueryDTO.builder().build());
        Chef targetChef = chefList.stream()
                .filter(chef -> chef.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (targetChef == null) {
            return null;
        }

        int rows = adminMapper.updateChefStatusById(id, statusUpdateDTO.getStatus(), LocalDateTime.now());
        if (rows <= 0) {
            return null;
        }

        targetChef.setStatus(statusUpdateDTO.getStatus());
        return toAdminChefVO(targetChef);
    }

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 后台综合管理服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
     */
    @Override
    public List<AdminOrderVO> getOrderList(AdminOrderQueryDTO queryDTO) {
        List<Order> orderList = adminMapper.selectOrderList(queryDTO);
        if (orderList == null || orderList.isEmpty()) {
            return Collections.emptyList();
        }
        return orderList.stream()
                .map(this::toAdminOrderVO)
                .collect(Collectors.toList());
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 后台综合管理服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        return toOrderDetailVO(orderMapper.selectById(id));
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 后台综合管理服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
    private AdminLoginVO toAdminLoginVO(Admin admin) {
        if (admin == null) {
            return null;
        }
        return AdminLoginVO.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .realName(admin.getRealName())
                .role(admin.getRole())
                .status(admin.getStatus())
                .build();
    }

    /**
     * 方法说明：在 后台综合管理服务实现 中处理 passwordMatches 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(storedPassword)) {
            return false;
        }
        if (storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 后台综合管理服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
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
                .gender(user.getGender())
                .tastePreference(user.getTastePreference())
                .status(user.getStatus())
                .statusDesc(UserStatusEnum.getDescByCode(user.getStatus()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 后台综合管理服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
    private AdminChefVO toAdminChefVO(Chef chef) {
        if (chef == null) {
            return null;
        }
        return AdminChefVO.builder()
                .id(chef.getId())
                .name(chef.getName())
                .phone(chef.getPhone())
                .specialtyCuisine(chef.getSpecialtyCuisine())
                .yearsOfExperience(chef.getYearsOfExperience())
                .ratingAvg(chef.getRatingAvg())
                .certStatus(chef.getCertStatus())
                .certStatusDesc(ChefCertStatusEnum.getDescByCode(chef.getCertStatus()))
                .status(chef.getStatus())
                .statusDesc(ChefStatusEnum.getDescByCode(chef.getStatus()))
                .createdAt(chef.getCreatedAt())
                .build();
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 后台综合管理服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
    private AdminOrderVO toAdminOrderVO(Order order) {
        if (order == null) {
            return null;
        }
        return AdminOrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .chefId(order.getChefId())
                .serviceDate(order.getServiceDate())
                .timeSlot(order.getTimeSlot())
                .timeSlotDesc(TimeSlotEnum.getDescByCode(order.getTimeSlot()))
                .payAmount(order.getPayAmount())
                .orderStatus(order.getOrderStatus())
                .contactName(order.getContactName())
                .contactPhone(order.getContactPhone())
                .fullAddress(order.getFullAddress())
                .createdAt(order.getCreatedAt())
                .build();
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 后台综合管理服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
    private OrderDetailVO toOrderDetailVO(Order order) {
        if (order == null) {
            return null;
        }
        return OrderDetailVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .chefId(order.getChefId())
                .addressId(order.getAddressId())
                .serviceDate(order.getServiceDate())
                .timeSlot(order.getTimeSlot())
                .timeSlotDesc(TimeSlotEnum.getDescByCode(order.getTimeSlot()))
                .serviceStartTime(order.getServiceStartTime())
                .serviceEndTime(order.getServiceEndTime())
                .peopleCount(order.getPeopleCount())
                .tastePreference(order.getTastePreference())
                .tabooFood(order.getTabooFood())
                .specialRequirement(order.getSpecialRequirement())
                .ingredientMode(order.getIngredientMode())
                .ingredientList(order.getIngredientList())
                .contactName(order.getContactName())
                .contactPhone(order.getContactPhone())
                .fullAddress(order.getFullAddress())
                .longitude(order.getLongitude())
                .latitude(order.getLatitude())
                .confirmCode(order.getConfirmCode())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .payAmount(order.getPayAmount())
                .orderStatus(order.getOrderStatus())
                .cancelReason(order.getCancelReason())
                .refundReason(order.getRefundReason())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
