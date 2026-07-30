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
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
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
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public UserAddressVO getDefaultAddress(Long userId) {
        return toUserAddressVO(userAddressMapper.selectDefaultByUserId(userId));
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public UserAddressVO getById(Long id) {
        return toUserAddressVO(userAddressMapper.selectById(id));
    }

    /**
     * 新建一条业务数据。
     * 这个方法用于把前端提交的新信息正式写入数据库。
     * 它会先做必要的检查和组装，再保存数据，最后返回新建后的结果。
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
     * 修改一条已有的业务数据。
     * 这个方法用于更新详情信息、状态或某些可编辑字段。
     * 它会先查出原始数据，再把新值写进去，最后保存并返回更新后的结果。
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
     * 设置某个业务字段的最终状态。
     * 这个方法通常用于切换默认值、启用状态或类似的标记位。
     * 它会先确认目标记录没问题，再把指定字段更新成目标状态。
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
     * 删除一条不再需要的数据。
     * 这个方法主要用来清理记录，避免无效数据继续留在系统里。
     * 它通常会先查询要删的数据，确认没问题后再执行删除。
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
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
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
