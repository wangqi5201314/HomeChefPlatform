package com.homechef.homechefsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;

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
}
