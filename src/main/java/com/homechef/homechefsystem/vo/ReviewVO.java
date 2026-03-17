package com.homechef.homechefsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewVO {

    private Long id;

    private Long orderId;

    private Long userId;

    private Long chefId;

    private Integer dishScore;

    private Integer serviceScore;

    private Integer skillScore;

    private Integer environmentScore;

    private BigDecimal overallScore;

    private String content;

    private String imageUrls;

    private Integer isAnonymous;

    private String replyContent;

    private LocalDateTime replyAt;

    private LocalDateTime createdAt;
}
