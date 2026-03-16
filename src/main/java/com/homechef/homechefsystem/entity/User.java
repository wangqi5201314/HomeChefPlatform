package com.homechef.homechefsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;

    private String openid;

    private String unionid;

    private String phone;

    private String nickname;

    private String avatar;

    private Integer gender;

    private LocalDate birthday;

    private String tastePreference;

    private String allergyInfo;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private Integer status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
