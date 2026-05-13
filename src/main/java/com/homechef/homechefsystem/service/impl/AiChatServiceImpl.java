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

    /**
     * 接收前端的 AI 聊天请求，并返回一条流式对话连接。
     * 这个方法让前端可以像聊天一样边发问题边看到 AI 一段段地回答。
     * 它会先组装要发给模型的消息，然后创建 SSE 连接，再异步调用模型推流返回结果。
     */
    @Override
    public SseEmitter chat(AiChatRequestDTO aiChatRequestDTO) {
        List<BailianClient.BailianMessage> messages = buildMessages(aiChatRequestDTO);

        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> streamChat(emitter, messages));
        return emitter;
    }

    /**
     * 把消息发给大模型，并把返回内容一段段推给前端。
     * 这个方法是 AI 对话真正开始运行的地方，负责把模型输出变成前端能直接接收的数据。
     * 它会调用模型客户端进行流式请求，每收到一段文本就发送 message 事件，全部完成后再发送 done 事件。
     */
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

    /**
     * 构建一个后续会被重复使用的中间结果。
     * 这个方法主要是为了把主流程里的细节拆出去，让主流程更容易看。
     * 它会根据当前需要把集合、映射、路径、文本或比较器等内容先准备好。
     */
    private List<BailianClient.BailianMessage> buildMessages(AiChatRequestDTO aiChatRequestDTO) {
        String message = aiChatRequestDTO.getMessage();
        if (!StringUtils.hasText(message)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "message can not be blank");
        }

        List<BailianClient.BailianMessage> messages = new ArrayList<>();
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

    /**
     * 构建一个后续会被重复使用的中间结果。
     * 这个方法主要是为了把主流程里的细节拆出去，让主流程更容易看。
     * 它会根据当前需要把集合、映射、路径、文本或比较器等内容先准备好。
     */
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

    /**
     * 发送一段要返回给外部的内容。
     * 这个方法把重复的发送逻辑单独拿出来，主流程会更清楚。
     * 它会先组装返回数据，再通过事件流或其他方式把内容发出去。
     */
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

    /**
     * 发送一段要返回给外部的内容。
     * 这个方法把重复的发送逻辑单独拿出来，主流程会更清楚。
     * 它会先组装返回数据，再通过事件流或其他方式把内容发出去。
     */
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
