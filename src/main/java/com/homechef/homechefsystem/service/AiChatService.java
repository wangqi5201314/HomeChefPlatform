package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.dto.AiChatRequestDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiChatService {

    /**
     * 发起 AI 对话并返回流式响应。
     */
    SseEmitter chat(AiChatRequestDTO aiChatRequestDTO);
}
