package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.dto.UserAddressCreateDTO;
import com.homechef.homechefsystem.dto.UserAddressQueryDTO;
import com.homechef.homechefsystem.dto.UserAddressUpdateDTO;
import com.homechef.homechefsystem.entity.UserAddress;
import com.homechef.homechefsystem.mapper.UserAddressMapper;
import com.homechef.homechefsystem.service.UserAddressService;
import com.homechef.homechefsystem.vo.UserAddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddressVO> getAddressList(UserAddressQueryDTO queryDTO) {
        List<UserAddress> userAddressList = userAddressMapper.selectList(queryDTO);
        if (userAddressList == null || userAddressList.isEmpty()) {
            return Collections.emptyList();
        }
        return userAddressList.stream()
                .map(this::toUserAddressVO)
                .collect(Collectors.toList());
    }

    @Override
    public UserAddressVO getDefaultAddress(Long userId) {
        return toUserAddressVO(userAddressMapper.selectDefaultByUserId(userId));
    }

    @Override
    public UserAddressVO getById(Long id) {
        return toUserAddressVO(userAddressMapper.selectById(id));
    }

    @Override
    public UserAddressVO create(UserAddressCreateDTO userAddressCreateDTO) {
        LocalDateTime now = LocalDateTime.now();
        int activeCount = userAddressMapper.countActiveByUserId(userAddressCreateDTO.getUserId());

        Integer isDefault = userAddressCreateDTO.getIsDefault();
        if (activeCount == 0) {
            isDefault = 1;
        } else if (isDefault == null) {
            isDefault = 0;
        }

        if (Integer.valueOf(1).equals(isDefault)) {
            userAddressMapper.resetDefaultByUserId(userAddressCreateDTO.getUserId(), now);
        }

        UserAddress userAddress = UserAddress.builder()
                .userId(userAddressCreateDTO.getUserId())
                .contactName(userAddressCreateDTO.getContactName())
                .contactPhone(userAddressCreateDTO.getContactPhone())
                .province(userAddressCreateDTO.getProvince())
                .city(userAddressCreateDTO.getCity())
                .district(userAddressCreateDTO.getDistrict())
                .detailAddress(userAddressCreateDTO.getDetailAddress())
                .longitude(userAddressCreateDTO.getLongitude())
                .latitude(userAddressCreateDTO.getLatitude())
                .doorplate(userAddressCreateDTO.getDoorplate())
                .isDefault(isDefault)
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();

        int rows = userAddressMapper.insert(userAddress);
        if (rows <= 0) {
            return null;
        }
        return toUserAddressVO(userAddressMapper.selectById(userAddress.getId()));
    }

    @Override
    public UserAddressVO updateById(Long id, UserAddressUpdateDTO userAddressUpdateDTO) {
        UserAddress existingUserAddress = userAddressMapper.selectById(id);
        if (existingUserAddress == null) {
            return null;
        }

        Integer isDefault = userAddressUpdateDTO.getIsDefault();
        if (isDefault == null) {
            isDefault = 0;
        }

        LocalDateTime now = LocalDateTime.now();
        if (Integer.valueOf(1).equals(isDefault)) {
            userAddressMapper.resetDefaultByUserId(existingUserAddress.getUserId(), now);
        }

        existingUserAddress.setContactName(userAddressUpdateDTO.getContactName());
        existingUserAddress.setContactPhone(userAddressUpdateDTO.getContactPhone());
        existingUserAddress.setProvince(userAddressUpdateDTO.getProvince());
        existingUserAddress.setCity(userAddressUpdateDTO.getCity());
        existingUserAddress.setDistrict(userAddressUpdateDTO.getDistrict());
        existingUserAddress.setDetailAddress(userAddressUpdateDTO.getDetailAddress());
        existingUserAddress.setLongitude(userAddressUpdateDTO.getLongitude());
        existingUserAddress.setLatitude(userAddressUpdateDTO.getLatitude());
        existingUserAddress.setDoorplate(userAddressUpdateDTO.getDoorplate());
        existingUserAddress.setIsDefault(isDefault);
        existingUserAddress.setUpdatedAt(now);

        int rows = userAddressMapper.updateById(existingUserAddress);
        if (rows <= 0) {
            return null;
        }
        return toUserAddressVO(userAddressMapper.selectById(id));
    }

    @Override
    public UserAddressVO setDefaultById(Long id, Long userId) {
        UserAddress existingUserAddress = userAddressMapper.selectById(id);
        if (existingUserAddress == null) {
            return null;
        }
        if (!existingUserAddress.getUserId().equals(userId)) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        userAddressMapper.resetDefaultByUserId(userId, now);
        int rows = userAddressMapper.setDefaultById(id, userId, now);
        if (rows <= 0) {
            return null;
        }
        return toUserAddressVO(userAddressMapper.selectById(id));
    }

    @Override
    public UserAddressVO deleteById(Long id) {
        UserAddress existingUserAddress = userAddressMapper.selectById(id);
        if (existingUserAddress == null) {
            return null;
        }

        int rows = userAddressMapper.logicDeleteById(id, LocalDateTime.now());
        if (rows <= 0) {
            return null;
        }
        return toUserAddressVO(existingUserAddress);
    }

    private UserAddressVO toUserAddressVO(UserAddress userAddress) {
        if (userAddress == null) {
            return null;
        }
        return UserAddressVO.builder()
                .id(userAddress.getId())
                .userId(userAddress.getUserId())
                .contactName(userAddress.getContactName())
                .contactPhone(userAddress.getContactPhone())
                .province(userAddress.getProvince())
                .city(userAddress.getCity())
                .district(userAddress.getDistrict())
                .detailAddress(userAddress.getDetailAddress())
                .longitude(userAddress.getLongitude())
                .latitude(userAddress.getLatitude())
                .doorplate(userAddress.getDoorplate())
                .isDefault(userAddress.getIsDefault())
                .build();
    }
}
