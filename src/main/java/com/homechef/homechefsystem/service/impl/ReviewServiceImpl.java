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
     * 方法说明：新增一条当前业务场景下的数据记录。
     * 主要作用：它承担 评价服务实现 中的新增入口，把前端入参转换为可持久化的实体数据。
     * 实现逻辑：实现逻辑通常会先校验关键字段和归属关系，再组装实体写入数据库，最后返回新增后的最新结果。
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
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 评价服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
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
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 评价服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
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
     * 方法说明：在 评价服务实现 中处理 replyById 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：判断指定业务数据是否已经存在。
     * 主要作用：该方法用于 评价服务实现 中的前置去重或存在性验证，避免重复创建或引用无效数据。
     * 实现逻辑：实现逻辑通常会根据主键、业务唯一键或关联条件调用 Mapper 统计结果，再把是否存在返回给上层流程使用。
     */
    @Override
    public boolean orderExists(Long orderId) {
        return orderMapper.selectById(orderId) != null;
    }

    /**
     * 方法说明：判断指定业务数据是否已经存在。
     * 主要作用：该方法用于 评价服务实现 中的前置去重或存在性验证，避免重复创建或引用无效数据。
     * 实现逻辑：实现逻辑通常会根据主键、业务唯一键或关联条件调用 Mapper 统计结果，再把是否存在返回给上层流程使用。
     */
    @Override
    public boolean existsByOrderId(Long orderId) {
        return reviewMapper.countByOrderId(orderId) > 0;
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 评价服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public ReviewVO getByOrderId(Long orderId) {
        return toReviewVO(reviewMapper.selectByOrderId(orderId));
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 评价服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
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
     * 方法说明：在 评价服务实现 中处理 calculateOverallScore 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：校验当前业务输入或状态是否满足执行条件。
     * 主要作用：它用于把 评价服务实现 中的前置规则集中收口，避免核心流程夹杂过多重复的条件判断。
     * 实现逻辑：实现逻辑会逐项检查关键字段、状态或业务约束，一旦发现不满足条件的情况就立即抛出业务异常阻断流程。
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
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 评价服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
