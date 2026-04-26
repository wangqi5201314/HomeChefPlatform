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

    /**
     * 方法说明：查询符合条件的列表数据。
     * 主要作用：它为 后台评价管理服务实现 提供页面列表、后台筛选或批量展示所需的数据集合。
     * 实现逻辑：实现逻辑通常是根据查询条件调用 Mapper 获取记录列表，再按需要转换为 VO 集合；当结果为空时会返回空集合或由上层统一处理。
     */
    @Override
    public List<AdminReviewVO> getReviewList(AdminReviewQueryDTO queryDTO) {
        List<Review> reviewList = reviewMapper.selectAdminList(queryDTO);
        if (reviewList == null || reviewList.isEmpty()) {
            return Collections.emptyList();
        }
        return reviewList.stream()
                .map(this::toAdminReviewVO)
                .collect(Collectors.toList());
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 后台评价管理服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    @Override
    public AdminReviewDetailVO getReviewDetail(Long id) {
        return toAdminReviewDetailVO(reviewMapper.selectById(id));
    }

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 后台评价管理服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 后台评价管理服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
