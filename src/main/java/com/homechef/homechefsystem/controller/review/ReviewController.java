package com.homechef.homechefsystem.controller.review;

import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.ReviewCreateDTO;
import com.homechef.homechefsystem.dto.ReviewReplyDTO;
import com.homechef.homechefsystem.service.ReviewService;
import com.homechef.homechefsystem.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/create")
    public Result<ReviewVO> create(@RequestBody ReviewCreateDTO reviewCreateDTO) {
        if (!reviewService.orderExists(reviewCreateDTO.getOrderId())) {
            return Result.error(404, "order not found");
        }
        if (reviewService.existsByOrderId(reviewCreateDTO.getOrderId())) {
            return Result.error(400, "review already exists");
        }

        ReviewVO reviewVO = reviewService.create(reviewCreateDTO);
        if (reviewVO == null) {
            return Result.error(500, "create review failed");
        }
        return Result.success(reviewVO);
    }

    @GetMapping("/chef/{chefId}")
    public Result<List<ReviewVO>> getChefReviewList(@PathVariable Long chefId) {
        return Result.success(reviewService.getChefReviewList(chefId));
    }

    @GetMapping("/user/{userId}")
    public Result<List<ReviewVO>> getUserReviewList(@PathVariable Long userId) {
        return Result.success(reviewService.getUserReviewList(userId));
    }

    @PostMapping("/{id}/reply")
    public Result<ReviewVO> replyById(@PathVariable Long id, @RequestBody ReviewReplyDTO reviewReplyDTO) {
        ReviewVO reviewVO = reviewService.replyById(id, reviewReplyDTO);
        if (reviewVO == null) {
            return Result.error(404, "review not found");
        }
        return Result.success(reviewVO);
    }
}
