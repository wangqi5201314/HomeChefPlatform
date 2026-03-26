package com.homechef.homechefsystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequestDTO {

    @NotBlank(message = "message can not be blank")
    private String message;

    @Valid
    private List<AiHistoryMessageDTO> history;
}
