package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.ReviewCreateDTO;
import com.homechef.homechefsystem.dto.ReviewReplyDTO;
import com.homechef.homechefsystem.vo.ReviewVO;

import java.util.List;

public interface ReviewService {

    ReviewVO create(ReviewCreateDTO reviewCreateDTO);

    List<ReviewVO> getChefReviewList(Long chefId);

    List<ReviewVO> getUserReviewList(Long userId);

    ReviewVO replyById(Long id, ReviewReplyDTO reviewReplyDTO);

    boolean orderExists(Long orderId);

    boolean existsByOrderId(Long orderId);

    ReviewVO getByOrderId(Long orderId);
}
