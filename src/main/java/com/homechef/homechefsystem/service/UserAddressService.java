package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.UserAddressCreateDTO;
import com.homechef.homechefsystem.dto.UserAddressQueryDTO;
import com.homechef.homechefsystem.dto.UserAddressUpdateDTO;
import com.homechef.homechefsystem.vo.UserAddressVO;

import java.util.List;

public interface UserAddressService {

    List<UserAddressVO> getAddressList(UserAddressQueryDTO queryDTO);

    UserAddressVO getDefaultAddress(Long userId);

    UserAddressVO getById(Long id);

    UserAddressVO create(UserAddressCreateDTO userAddressCreateDTO);

    UserAddressVO updateById(Long id, UserAddressUpdateDTO userAddressUpdateDTO);

    UserAddressVO setDefaultById(Long id, Long userId);

    UserAddressVO deleteById(Long id);
}
