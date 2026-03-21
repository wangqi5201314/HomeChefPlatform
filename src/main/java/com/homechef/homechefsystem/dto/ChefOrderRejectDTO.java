package com.homechef.homechefsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefOrderRejectDTO {

    @NotBlank(message = "reason can not be blank")
    private String reason;
}
