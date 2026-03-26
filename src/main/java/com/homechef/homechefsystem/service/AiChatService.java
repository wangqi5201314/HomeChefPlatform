package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.AiChatRequestDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiChatService {

    SseEmitter chat(AiChatRequestDTO aiChatRequestDTO);
}
