package com.homechef.homechefsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateDTO {

    private Long orderId;

    private Long userId;

    private Long chefId;

    private Integer dishScore;

    private Integer serviceScore;

    private Integer skillScore;

    private Integer environmentScore;

    private String content;

    private String imageUrls;

    private Integer isAnonymous;
}
