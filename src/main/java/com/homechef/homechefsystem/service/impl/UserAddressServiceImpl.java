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

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 用户地址服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
     */
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

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 用户地址服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public UserAddressVO getDefaultAddress(Long userId) {
        return toUserAddressVO(userAddressMapper.selectDefaultByUserId(userId));
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 用户地址服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public UserAddressVO getById(Long id) {
        return toUserAddressVO(userAddressMapper.selectById(id));
    }

    /**
     * 方法说明：新增一条当前业务场景下的数据记录。
     * 主要作用：它承担 用户地址服务实现 中的新增入口，把前端入参转换为可持久化的实体数据。
     * 实现逻辑：实现逻辑通常会先校验关键字段和归属关系，再组装实体写入数据库，最后返回新增后的最新结果。
     */
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
                .town(userAddressCreateDTO.getTown())
                .detailAddress(userAddressCreateDTO.getDetailAddress())
                .longitude(userAddressCreateDTO.getLongitude())
                .latitude(userAddressCreateDTO.getLatitude())
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

    /**
     * 方法说明：更新一条当前业务场景下的数据记录或状态。
     * 主要作用：该方法用于 用户地址服务实现 中的编辑、状态变更或流程推进，保证外部只能修改业务允许变动的部分。
     * 实现逻辑：实现时会先查询原始数据并做归属或状态校验，再回填可编辑字段执行更新，必要时返回更新后的详情结果。
     */
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
        existingUserAddress.setTown(userAddressUpdateDTO.getTown());
        existingUserAddress.setDetailAddress(userAddressUpdateDTO.getDetailAddress());
        existingUserAddress.setLongitude(userAddressUpdateDTO.getLongitude());
        existingUserAddress.setLatitude(userAddressUpdateDTO.getLatitude());
        existingUserAddress.setIsDefault(isDefault);
        existingUserAddress.setUpdatedAt(now);

        int rows = userAddressMapper.updateById(existingUserAddress);
        if (rows <= 0) {
            return null;
        }
        return toUserAddressVO(userAddressMapper.selectById(id));
    }

    /**
     * 方法说明：在 用户地址服务实现 中处理 setDefaultById 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
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

    /**
     * 方法说明：删除当前业务场景下指定的数据记录。
     * 主要作用：它为 用户地址服务实现 提供清理数据的能力，同时确保删除动作仍然符合归属、状态或业务规则限制。
     * 实现逻辑：实现逻辑通常会先查询并校验目标记录，再执行删除；若前端需要回显删除前信息，则会在删除前先转换出返回对象。
     */
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

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 用户地址服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
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
                .town(userAddress.getTown())
                .detailAddress(userAddress.getDetailAddress())
                .longitude(userAddress.getLongitude())
                .latitude(userAddress.getLatitude())
                .isDefault(userAddress.getIsDefault())
                .build();
    }
}
