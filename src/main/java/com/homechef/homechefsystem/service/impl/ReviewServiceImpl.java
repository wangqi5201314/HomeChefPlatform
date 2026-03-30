package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.OrderStatusEnum;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.ReviewCreateDTO;
import com.homechef.homechefsystem.dto.ReviewQueryDTO;
import com.homechef.homechefsystem.dto.ReviewReplyDTO;
import com.homechef.homechefsystem.entity.Order;
import com.homechef.homechefsystem.entity.Review;
import com.homechef.homechefsystem.entity.User;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.OrderMapper;
import com.homechef.homechefsystem.mapper.ReviewMapper;
import com.homechef.homechefsystem.mapper.UserMapper;
import com.homechef.homechefsystem.service.ReviewService;
import com.homechef.homechefsystem.utils.LoginUserContext;
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
    private final UserMapper userMapper;

    @Override
    @Transactional
    public ReviewVO create(ReviewCreateDTO reviewCreateDTO) {
        if (reviewCreateDTO.getOrderId() == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "orderId不能为空");
        }

        Order order = orderMapper.selectById(reviewCreateDTO.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "订单不存在");
        }

        Long currentUserId = LoginUserContext.getUserId();
        Long effectiveUserId = currentUserId != null ? currentUserId : reviewCreateDTO.getUserId();
        if (effectiveUserId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        if (!effectiveUserId.equals(order.getUserId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "只能评价自己的订单");
        }
        if (!OrderStatusEnum.COMPLETED.equalsCode(order.getOrderStatus())) {
            throw new BusinessException(ResultCodeEnum.FAIL, "仅已完成订单允许评价");
        }
        if (reviewMapper.countByOrderId(reviewCreateDTO.getOrderId()) > 0) {
            throw new BusinessException(ResultCodeEnum.FAIL, "该订单已评价，不能重复评价");
        }

        LocalDateTime now = LocalDateTime.now();
        Integer isAnonymous = reviewCreateDTO.getIsAnonymous();
        if (isAnonymous == null) {
            isAnonymous = 0;
        }

        Review review = Review.builder()
                .orderId(reviewCreateDTO.getOrderId())
                .userId(effectiveUserId)
                .chefId(order.getChefId())
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
        int updatedRows = chefMapper.updateReviewStatsById(order.getChefId(), now);
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

    @Override
    public ReviewVO getByOrderId(Long orderId) {
        return toReviewVO(reviewMapper.selectByOrderId(orderId));
    }

    private BigDecimal calculateOverallScore(Integer dishScore,
                                             Integer serviceScore,
                                             Integer skillScore,
                                             Integer environmentScore) {
        validateScore(dishScore, "dishScore");
        validateScore(serviceScore, "serviceScore");
        validateScore(skillScore, "skillScore");
        validateScore(environmentScore, "environmentScore");
        BigDecimal total = BigDecimal.valueOf(dishScore)
                .add(BigDecimal.valueOf(serviceScore))
                .add(BigDecimal.valueOf(skillScore))
                .add(BigDecimal.valueOf(environmentScore));
        return total.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
    }

    private void validateScore(Integer score, String fieldName) {
        if (score == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, fieldName + "不能为空");
        }
        if (score < 1 || score > 5) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, fieldName + "必须在1到5之间");
        }
    }

    private ReviewVO toReviewVO(Review review) {
        if (review == null) {
            return null;
        }
        User user = userMapper.selectById(review.getUserId());
        return ReviewVO.builder()
                .id(review.getId())
                .orderId(review.getOrderId())
                .userId(review.getUserId())
                .userName(user == null ? null : user.getNickname())
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
