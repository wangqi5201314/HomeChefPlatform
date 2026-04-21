package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.client.BailianClient;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.AiChatRequestDTO;
import com.homechef.homechefsystem.dto.AiHistoryMessageDTO;
import com.homechef.homechefsystem.service.AiChatService;
import com.homechef.homechefsystem.vo.AiChatResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private static final int MAX_HISTORY_MESSAGES = 20;

    private static final String SYSTEM_PROMPT = "你是小嘉AI，是私房菜上门服务系统中的美食与厨艺助手。"
            + "你只回答与菜品、做法、烹饪技巧、火候、调味、食材搭配、厨房常识、家常菜建议相关的问题。"
            + "回答要中文、清晰、实用、简洁。若用户问题明显与美食厨艺无关，请礼貌提示你主要提供菜品与厨艺相关帮助。";

    private final BailianClient bailianClient;

    @Override
    public SseEmitter chat(AiChatRequestDTO aiChatRequestDTO) {
        List<BailianClient.BailianMessage> messages = buildMessages(aiChatRequestDTO);

        // SSE 连接不能阻塞 Controller 线程，所以异步调用模型并持续向前端推送分片内容。
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> streamChat(emitter, messages));
        return emitter;
    }

    private void streamChat(SseEmitter emitter, List<BailianClient.BailianMessage> messages) {
        try {
            bailianClient.streamChat(messages, chunk -> sendMessageChunk(emitter, chunk));
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data(Result.success()));
            emitter.complete();
        } catch (Exception e) {
            sendErrorEvent(emitter, e);
        }
    }

    private List<BailianClient.BailianMessage> buildMessages(AiChatRequestDTO aiChatRequestDTO) {
        String message = aiChatRequestDTO.getMessage();
        if (!StringUtils.hasText(message)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "message can not be blank");
        }

        List<BailianClient.BailianMessage> messages = new ArrayList<>();
        // system prompt 用来固定“小嘉AI”的角色和回答边界，避免模型变成通用闲聊助手。
        messages.add(BailianClient.BailianMessage.builder()
                .role("system")
                .content(SYSTEM_PROMPT)
                .build());
        messages.addAll(buildHistoryMessages(aiChatRequestDTO.getHistory()));
        messages.add(BailianClient.BailianMessage.builder()
                .role("user")
                .content(message.trim())
                .build());
        return messages;
    }

    private List<BailianClient.BailianMessage> buildHistoryMessages(List<AiHistoryMessageDTO> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }

        List<AiHistoryMessageDTO> validHistory = history.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getContent()))
                .toList();
        if (validHistory.isEmpty()) {
            return Collections.emptyList();
        }

        int fromIndex = Math.max(0, validHistory.size() - MAX_HISTORY_MESSAGES);
        // 只携带最近若干条上下文，既保留多轮对话能力，也控制模型请求长度。
        List<AiHistoryMessageDTO> recentHistory = validHistory.subList(fromIndex, validHistory.size());

        List<BailianClient.BailianMessage> messages = new ArrayList<>(recentHistory.size());
        for (AiHistoryMessageDTO item : recentHistory) {
            String role = item.getRole() == null ? null : item.getRole().trim();
            if (!"user".equals(role) && !"assistant".equals(role)) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "history role 只能为 user 或 assistant");
            }
            messages.add(BailianClient.BailianMessage.builder()
                    .role(role)
                    .content(item.getContent().trim())
                    .build());
        }
        return messages;
    }

    private void sendMessageChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(Result.success(AiChatResponseVO.builder()
                            .reply(chunk)
                            .build())));
        } catch (IOException e) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "failed to send ai stream chunk");
        }
    }

    private void sendErrorEvent(SseEmitter emitter, Exception exception) {
        String message = exception instanceof BusinessException
                ? exception.getMessage()
                : "ai stream failed";
        Integer code = exception instanceof BusinessException
                ? ((BusinessException) exception).getCode()
                : ResultCodeEnum.SYSTEM_ERROR.getCode();
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Result.error(code, message)));
        } catch (IOException ignored) {
        } finally {
            emitter.complete();
        }
    }
}
