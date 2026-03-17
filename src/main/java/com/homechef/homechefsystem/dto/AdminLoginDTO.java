package com.homechef.homechefsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginDTO {

    @NotBlank(message = "username can not be blank")
    @Size(max = 50, message = "username length must be less than or equal to 50")
    private String username;

    @NotBlank(message = "password can not be blank")
    @Size(max = 255, message = "password length must be less than or equal to 255")
    private String password;
}
