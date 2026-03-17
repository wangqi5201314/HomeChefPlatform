package com.homechef.homechefsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    private Long id;

    private String username;

    private String password;

    private String realName;

    private String role;

    private Integer status;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
