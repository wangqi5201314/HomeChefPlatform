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
public class ChefRegisterDTO {

    @NotBlank(message = "phone can not be blank")
    @Size(max = 20, message = "phone length must be less than or equal to 20")
    private String phone;

    @NotBlank(message = "password can not be blank")
    @Size(min = 6, message = "password length must be greater than or equal to 6")
    private String password;

    @NotBlank(message = "confirmPassword can not be blank")
    private String confirmPassword;

    private String name;
}
