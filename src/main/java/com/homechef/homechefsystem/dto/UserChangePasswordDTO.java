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
public class UserChangePasswordDTO {

    @NotBlank(message = "oldPassword can not be blank")
    private String oldPassword;

    @NotBlank(message = "newPassword can not be blank")
    @Size(min = 6, message = "newPassword length must be greater than or equal to 6")
    private String newPassword;

    @NotBlank(message = "confirmPassword can not be blank")
    private String confirmPassword;
}
