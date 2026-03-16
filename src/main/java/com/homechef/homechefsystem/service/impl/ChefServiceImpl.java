package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.dto.ChefQueryDTO;
import com.homechef.homechefsystem.dto.ChefUpdateDTO;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.service.ChefService;
import com.homechef.homechefsystem.vo.ChefDetailVO;
import com.homechef.homechefsystem.vo.ChefListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChefServiceImpl implements ChefService {

    private final ChefMapper chefMapper;

    @Override
    public List<ChefListVO> getChefList(ChefQueryDTO queryDTO) {
        List<Chef> chefList = chefMapper.selectList(queryDTO);
        if (chefList == null || chefList.isEmpty()) {
            return Collections.emptyList();
        }
        return chefList.stream()
                .map(this::toChefListVO)
                .collect(Collectors.toList());
    }

    @Override
    public ChefDetailVO getById(Long id) {
        return toChefDetailVO(chefMapper.selectById(id));
    }

    @Override
    public ChefDetailVO updateById(Long id, ChefUpdateDTO chefUpdateDTO) {
        Chef existingChef = chefMapper.selectById(id);
        if (existingChef == null) {
            return null;
        }

        existingChef.setName(chefUpdateDTO.getName());
        existingChef.setPhone(chefUpdateDTO.getPhone());
        existingChef.setAvatar(chefUpdateDTO.getAvatar());
        existingChef.setGender(chefUpdateDTO.getGender());
        existingChef.setAge(chefUpdateDTO.getAge());
        existingChef.setIntroduction(chefUpdateDTO.getIntroduction());
        existingChef.setSpecialtyCuisine(chefUpdateDTO.getSpecialtyCuisine());
        existingChef.setSpecialtyTags(chefUpdateDTO.getSpecialtyTags());
        existingChef.setYearsOfExperience(chefUpdateDTO.getYearsOfExperience());
        existingChef.setServiceRadiusKm(chefUpdateDTO.getServiceRadiusKm());
        existingChef.setServiceMode(chefUpdateDTO.getServiceMode());
        existingChef.setStatus(chefUpdateDTO.getStatus());
        existingChef.setUpdatedAt(LocalDateTime.now());

        int rows = chefMapper.updateById(existingChef);
        if (rows <= 0) {
            return null;
        }
        return toChefDetailVO(chefMapper.selectById(id));
    }

    private ChefListVO toChefListVO(Chef chef) {
        if (chef == null) {
            return null;
        }
        return ChefListVO.builder()
                .id(chef.getId())
                .name(chef.getName())
                .avatar(chef.getAvatar())
                .specialtyCuisine(chef.getSpecialtyCuisine())
                .yearsOfExperience(chef.getYearsOfExperience())
                .ratingAvg(chef.getRatingAvg())
                .orderCount(chef.getOrderCount())
                .certStatus(chef.getCertStatus())
                .status(chef.getStatus())
                .build();
    }

    private ChefDetailVO toChefDetailVO(Chef chef) {
        if (chef == null) {
            return null;
        }
        return ChefDetailVO.builder()
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
                .ratingAvg(chef.getRatingAvg())
                .orderCount(chef.getOrderCount())
                .onTimeRate(chef.getOnTimeRate())
                .goodReviewRate(chef.getGoodReviewRate())
                .certStatus(chef.getCertStatus())
                .status(chef.getStatus())
                .build();
    }
}
