package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ReviewCreateDTO;
import com.homechef.homechefsystem.dto.ReviewReplyDTO;
import com.homechef.homechefsystem.vo.ReviewVO;

import java.util.List;

public interface ReviewService {

    /**
     * 创建数据并返回处理结果。
     */
    ReviewVO create(ReviewCreateDTO reviewCreateDTO);

    /**
     * 查询列表数据并返回结果。
     */
    List<ReviewVO> getChefReviewList(Long chefId);

    /**
     * 查询列表数据并返回结果。
     */
    List<ReviewVO> getUserReviewList(Long userId);

    /**
     * 根据 ID 回复指定记录。
     */
    ReviewVO replyById(Long id, ReviewReplyDTO reviewReplyDTO);

    /**
     * 判断指定订单是否存在。
     */
    boolean orderExists(Long orderId);

    /**
     * 判断指定订单是否已经存在对应记录。
     */
    boolean existsByOrderId(Long orderId);

    /**
     * 根据订单 ID 查询对应数据。
     */
    ReviewVO getByOrderId(Long orderId);

    /**
     * 根据订单编号查询对应数据。
     */
    ReviewVO getByOrderNo(String orderNo);
}
