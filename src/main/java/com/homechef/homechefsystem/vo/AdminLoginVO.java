package com.homechef.homechefsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginVO {

    private Long id;

    private String username;

    private String realName;

    private String role;

    private Integer status;
}
