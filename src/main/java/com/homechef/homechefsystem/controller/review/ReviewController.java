package com.homechef.homechefsystem.controller.review;

import com.homechef.homechefsystem.annotation.RequireLogin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.ReviewCreateDTO;
import com.homechef.homechefsystem.dto.ReviewReplyDTO;
import com.homechef.homechefsystem.service.ReviewService;
import com.homechef.homechefsystem.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "评价接口")
public class ReviewController {

    private final ReviewService reviewService;

    @RequireLogin
    @Operation(summary = "创建评价")
    @PostMapping("/create")
    public Result<ReviewVO> create(@RequestBody ReviewCreateDTO reviewCreateDTO) {
        ReviewVO reviewVO = reviewService.create(reviewCreateDTO);
        if (reviewVO == null) {
            return Result.error(500, "create review failed");
        }
        return Result.success(reviewVO);
    }

    @Operation(summary = "查询厨师评价列表")
    @GetMapping("/chef/{chefId}")
    public Result<List<ReviewVO>> getChefReviewList(@PathVariable Long chefId) {
        return Result.success(reviewService.getChefReviewList(chefId));
    }

    @Operation(summary = "查询用户评价列表")
    @GetMapping("/user/{userId}")
    public Result<List<ReviewVO>> getUserReviewList(@PathVariable Long userId) {
        return Result.success(reviewService.getUserReviewList(userId));
    }

    @Operation(summary = "回复评价")
    @PostMapping("/{id}/reply")
    public Result<ReviewVO> replyById(@PathVariable Long id, @RequestBody ReviewReplyDTO reviewReplyDTO) {
        ReviewVO reviewVO = reviewService.replyById(id, reviewReplyDTO);
        if (reviewVO == null) {
            return Result.error(404, "review not found");
        }
        return Result.success(reviewVO);
    }
}
