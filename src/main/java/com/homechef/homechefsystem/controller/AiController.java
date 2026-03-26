package com.homechef.homechefsystem.controller;

import com.homechef.homechefsystem.dto.AiChatRequestDTO;
import com.homechef.homechefsystem.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI 对话接口")
public class AiController {

    private final AiChatService aiChatService;

    @Operation(summary = "AI 流式对话")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AiChatRequestDTO aiChatRequestDTO) {
        return aiChatService.chat(aiChatRequestDTO);
    }
}
