package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.AdminReviewQueryDTO;
import com.homechef.homechefsystem.vo.AdminReviewDetailVO;
import com.homechef.homechefsystem.vo.AdminReviewVO;

import java.util.List;

public interface AdminReviewService {

    /**
     * 查询列表数据并返回结果。
     */
    List<AdminReviewVO> getReviewList(AdminReviewQueryDTO queryDTO);

    /**
     * 查询详情数据并返回结果。
     */
    AdminReviewDetailVO getReviewDetail(Long id);
}
