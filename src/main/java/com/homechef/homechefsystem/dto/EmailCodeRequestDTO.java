package com.homechef.homechefsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailCodeRequestDTO {

    @NotBlank(message = "email can not be blank")
    @Email(message = "email format is invalid")
    @Size(max = 128, message = "email length must be less than or equal to 128")
    private String email;
}
