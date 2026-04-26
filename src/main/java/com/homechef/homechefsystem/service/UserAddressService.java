package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.UserAddressCreateDTO;
import com.homechef.homechefsystem.dto.UserAddressQueryDTO;
import com.homechef.homechefsystem.dto.UserAddressUpdateDTO;
import com.homechef.homechefsystem.vo.UserAddressVO;

import java.util.List;

public interface UserAddressService {

    /**
     * 查询列表数据并返回结果。
     */
    List<UserAddressVO> getAddressList(UserAddressQueryDTO queryDTO);

    /**
     * 获取用户的默认地址。
     */
    UserAddressVO getDefaultAddress(Long userId);

    /**
     * 根据 ID 查询对应数据。
     */
    UserAddressVO getById(Long id);

    /**
     * 创建数据并返回处理结果。
     */
    UserAddressVO create(UserAddressCreateDTO userAddressCreateDTO);

    /**
     * 根据 ID 更新数据并返回结果。
     */
    UserAddressVO updateById(Long id, UserAddressUpdateDTO userAddressUpdateDTO);

    /**
     * 将指定地址设置为默认地址。
     */
    UserAddressVO setDefaultById(Long id, Long userId);

    /**
     * 根据 ID 删除指定数据并返回结果。
     */
    UserAddressVO deleteById(Long id);
}
