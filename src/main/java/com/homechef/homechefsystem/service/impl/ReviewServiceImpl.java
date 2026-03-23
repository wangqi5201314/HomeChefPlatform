package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.dto.ReviewCreateDTO;
import com.homechef.homechefsystem.dto.ReviewQueryDTO;
import com.homechef.homechefsystem.dto.ReviewReplyDTO;
import com.homechef.homechefsystem.entity.Review;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.OrderMapper;
import com.homechef.homechefsystem.mapper.ReviewMapper;
import com.homechef.homechefsystem.service.ReviewService;
import com.homechef.homechefsystem.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;

    private final OrderMapper orderMapper;

    private final ChefMapper chefMapper;

    @Override
    @Transactional
    public ReviewVO create(ReviewCreateDTO reviewCreateDTO) {
        LocalDateTime now = LocalDateTime.now();
        Integer isAnonymous = reviewCreateDTO.getIsAnonymous();
        if (isAnonymous == null) {
            isAnonymous = 0;
        }

        Review review = Review.builder()
                .orderId(reviewCreateDTO.getOrderId())
                .userId(reviewCreateDTO.getUserId())
                .chefId(reviewCreateDTO.getChefId())
                .dishScore(reviewCreateDTO.getDishScore())
                .serviceScore(reviewCreateDTO.getServiceScore())
                .skillScore(reviewCreateDTO.getSkillScore())
                .environmentScore(reviewCreateDTO.getEnvironmentScore())
                .overallScore(calculateOverallScore(
                        reviewCreateDTO.getDishScore(),
                        reviewCreateDTO.getServiceScore(),
                        reviewCreateDTO.getSkillScore(),
                        reviewCreateDTO.getEnvironmentScore()))
                .content(reviewCreateDTO.getContent())
                .imageUrls(reviewCreateDTO.getImageUrls())
                .isAnonymous(isAnonymous)
                .replyContent(null)
                .replyAt(null)
                .createdAt(now)
                .build();

        int rows = reviewMapper.insert(review);
        if (rows <= 0) {
            return null;
        }
        int updatedRows = chefMapper.updateReviewStatsById(reviewCreateDTO.getChefId(), now);
        if (updatedRows <= 0) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "update chef review stats failed");
        }
        return toReviewVO(reviewMapper.selectById(review.getId()));
    }

    @Override
    public List<ReviewVO> getChefReviewList(Long chefId) {
        List<Review> reviewList = reviewMapper.selectList(ReviewQueryDTO.builder().chefId(chefId).build());
        if (reviewList == null || reviewList.isEmpty()) {
            return Collections.emptyList();
        }
        return reviewList.stream()
                .map(this::toReviewVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewVO> getUserReviewList(Long userId) {
        List<Review> reviewList = reviewMapper.selectList(ReviewQueryDTO.builder().userId(userId).build());
        if (reviewList == null || reviewList.isEmpty()) {
            return Collections.emptyList();
        }
        return reviewList.stream()
                .map(this::toReviewVO)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewVO replyById(Long id, ReviewReplyDTO reviewReplyDTO) {
        Review existingReview = reviewMapper.selectById(id);
        if (existingReview == null) {
            return null;
        }

        int rows = reviewMapper.updateReplyById(id, reviewReplyDTO.getReplyContent(), LocalDateTime.now());
        if (rows <= 0) {
            return null;
        }
        return toReviewVO(reviewMapper.selectById(id));
    }

    @Override
    public boolean orderExists(Long orderId) {
        return orderMapper.selectById(orderId) != null;
    }

    @Override
    public boolean existsByOrderId(Long orderId) {
        return reviewMapper.countByOrderId(orderId) > 0;
    }

    private BigDecimal calculateOverallScore(Integer dishScore,
                                             Integer serviceScore,
                                             Integer skillScore,
                                             Integer environmentScore) {
        BigDecimal total = BigDecimal.valueOf(dishScore)
                .add(BigDecimal.valueOf(serviceScore))
                .add(BigDecimal.valueOf(skillScore))
                .add(BigDecimal.valueOf(environmentScore));
        return total.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
    }

    private ReviewVO toReviewVO(Review review) {
        if (review == null) {
            return null;
        }
        return ReviewVO.builder()
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
