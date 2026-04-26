package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.dto.AdminReviewQueryDTO;
import com.homechef.homechefsystem.entity.Review;
import com.homechef.homechefsystem.mapper.ReviewMapper;
import com.homechef.homechefsystem.service.AdminReviewService;
import com.homechef.homechefsystem.vo.AdminReviewDetailVO;
import com.homechef.homechefsystem.vo.AdminReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReviewServiceImpl implements AdminReviewService {

    private final ReviewMapper reviewMapper;

    @Override
    /**
     * 查询列表数据并返回结果。
     */
    public List<AdminReviewVO> getReviewList(AdminReviewQueryDTO queryDTO) {
        List<Review> reviewList = reviewMapper.selectAdminList(queryDTO);
        if (reviewList == null || reviewList.isEmpty()) {
            return Collections.emptyList();
        }
        return reviewList.stream()
                .map(this::toAdminReviewVO)
                .collect(Collectors.toList());
    }

    @Override
    /**
     * 查询详情数据并返回结果。
     */
    public AdminReviewDetailVO getReviewDetail(Long id) {
        return toAdminReviewDetailVO(reviewMapper.selectById(id));
    }

    /**
     * 将实体对象转换为前端返回 VO。
     */
    private AdminReviewVO toAdminReviewVO(Review review) {
        if (review == null) {
            return null;
        }
        return AdminReviewVO.builder()
                .id(review.getId())
                .orderId(review.getOrderId())
                .userId(review.getUserId())
                .chefId(review.getChefId())
                .dishScore(review.getDishScore())
                .serviceScore(review.getServiceScore())
                .skillScore(review.getSkillScore())
                .environmentScore(review.getEnvironmentScore())
                .overallScore(review.getOverallScore())
                .content(review.getContent())
                .imageUrls(review.getImageUrls())
                .isAnonymous(review.getIsAnonymous())
                .replyContent(review.getReplyContent())
                .replyAt(review.getReplyAt())
                .createdAt(review.getCreatedAt())
                .build();
    }

    /**
     * 将实体对象转换为前端返回 VO。
     */
    private AdminReviewDetailVO toAdminReviewDetailVO(Review review) {
        if (review == null) {
            return null;
        }
        return AdminReviewDetailVO.builder()
                .id(review.getId())
                .orderId(review.getOrderId())
                .userId(review.getUserId())
                .chefId(review.getChefId())
                .dishScore(review.getDishScore())
                .serviceScore(review.getServiceScore())
                .skillScore(review.getSkillScore())
                .environmentScore(review.getEnvironmentScore())
                .overallScore(review.getOverallScore())
                .content(review.getContent())
                .imageUrls(review.getImageUrls())
                .isAnonymous(review.getIsAnonymous())
                .replyContent(review.getReplyContent())
                .replyAt(review.getReplyAt())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
