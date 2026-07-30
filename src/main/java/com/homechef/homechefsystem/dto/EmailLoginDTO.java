package com.homechef.homechefsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailLoginDTO {

    @NotBlank(message = "email can not be blank")
    @Email(message = "email format is invalid")
    @Size(max = 128, message = "email length must be less than or equal to 128")
    private String email;

    @NotBlank(message = "code can not be blank")
    @Pattern(regexp = "\\d{6}", message = "code must be a 6 digit number")
    private String code;
}
