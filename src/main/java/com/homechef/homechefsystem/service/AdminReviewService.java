package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.AdminReviewQueryDTO;
import com.homechef.homechefsystem.vo.AdminReviewDetailVO;
import com.homechef.homechefsystem.vo.AdminReviewVO;

import java.util.List;

public interface AdminReviewService {

    List<AdminReviewVO> getReviewList(AdminReviewQueryDTO queryDTO);

    AdminReviewDetailVO getReviewDetail(Long id);
}
