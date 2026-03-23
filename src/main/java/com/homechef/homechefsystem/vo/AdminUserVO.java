package com.homechef.homechefsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserVO {

    private Long id;

    private String phone;

    private String nickname;

    private Integer gender;

    private String tastePreference;

    private Integer status;

    private String statusDesc;

    private LocalDateTime createdAt;
}
