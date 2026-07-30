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

    /**
     * 新建一条业务数据。
     * 这个方法用于把前端提交的新信息正式写入数据库。
     * 它会先做必要的检查和组装，再保存数据，最后返回新建后的结果。
     */
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

    /**
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
     */
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

    /**
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
     */
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

    /**
     * 处理 replyById 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
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

    /**
     * 判断某条数据是否已经存在。
     * 这个方法主要用于创建前去重，或者先判断关联数据是不是有效。
     * 它会按给定条件去查数据库，然后把是否存在的结果返回出来。
     */
    @Override
    public boolean orderExists(Long orderId) {
        return orderMapper.selectById(orderId) != null;
    }

    /**
     * 判断某条数据是否已经存在。
     * 这个方法主要用于创建前去重，或者先判断关联数据是不是有效。
     * 它会按给定条件去查数据库，然后把是否存在的结果返回出来。
     */
    @Override
    public boolean existsByOrderId(Long orderId) {
        return reviewMapper.countByOrderId(orderId) > 0;
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public ReviewVO getByOrderId(Long orderId) {
        return toReviewVO(reviewMapper.selectByOrderId(orderId));
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public ReviewVO getByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return null;
        }
        Order order = orderMapper.selectByOrderNo(orderNo.trim());
        if (order == null) {
            return null;
        }
        return toReviewVO(reviewMapper.selectByOrderId(order.getId()));
    }

    /**
     * 处理 calculateOverallScore 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
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

    /**
     * 检查当前传入的参数或业务状态是否合法。
     * 这个方法的作用，是把不合条件的情况尽早拦住，不让错误数据继续往下执行。
     * 它会根据规则逐项检查参数或状态，只要发现不满足条件，就直接抛出异常。
     */
    private void validateScore(Integer score, String fieldName) {
        if (score == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, fieldName + "不能为空");
        }
        if (score < 1 || score > 5) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, fieldName + "必须在1到5之间");
        }
    }

    /**
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
     */
    private ReviewVO toReviewVO(Review review) {
        if (review == null) {
            return null;
        }
        Order order = orderMapper.selectById(review.getOrderId());
        User user = userMapper.selectById(review.getUserId());
        return ReviewVO.builder()
                .id(review.getId())
                .orderId(review.getOrderId())
                .orderNo(order == null ? null : order.getOrderNo())
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
