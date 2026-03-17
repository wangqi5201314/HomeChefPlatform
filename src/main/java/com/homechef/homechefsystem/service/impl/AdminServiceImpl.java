package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.dto.AdminChefQueryDTO;
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
import com.homechef.homechefsystem.vo.AdminChefVO;
import com.homechef.homechefsystem.vo.AdminLoginVO;
import com.homechef.homechefsystem.vo.AdminOrderVO;
import com.homechef.homechefsystem.vo.AdminUserVO;
import com.homechef.homechefsystem.vo.OrderDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;

    private final OrderMapper orderMapper;

    @Override
    public AdminLoginVO login(AdminLoginDTO adminLoginDTO) {
        Admin admin = adminMapper.selectByUsername(adminLoginDTO.getUsername());
        if (admin == null) {
            return null;
        }
        if (!admin.getPassword().equals(adminLoginDTO.getPassword())) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        adminMapper.updateLoginTimeById(admin.getId(), now, now);
        return toAdminLoginVO(adminMapper.selectByUsername(adminLoginDTO.getUsername()));
    }

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

    @Override
    public AdminUserVO updateUserStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO) {
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

    @Override
    public AdminChefVO updateChefStatus(Long id, AdminStatusUpdateDTO statusUpdateDTO) {
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

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        return toOrderDetailVO(orderMapper.selectById(id));
    }

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
                .createdAt(user.getCreatedAt())
                .build();
    }

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
                .status(chef.getStatus())
                .createdAt(chef.getCreatedAt())
                .build();
    }

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
                .payAmount(order.getPayAmount())
                .orderStatus(order.getOrderStatus())
                .contactName(order.getContactName())
                .contactPhone(order.getContactPhone())
                .fullAddress(order.getFullAddress())
                .createdAt(order.getCreatedAt())
                .build();
    }

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
