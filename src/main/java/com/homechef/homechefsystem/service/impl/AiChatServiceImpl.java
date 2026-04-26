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
     * 方法说明：接收前端聊天请求并开启一条面向小程序的 AI 流式会话。
     * 主要作用：这是“小嘉AI”对外暴露的核心入口，用来把用户问题和历史上下文整理后，以 SSE 的方式实时返回模型输出。
     * 实现逻辑：方法会先构建包含系统提示词、历史消息和当前提问的消息列表，再创建 SseEmitter，并在异步线程中调用流式对话处理逻辑。
     */
    @Override
    public SseEmitter chat(AiChatRequestDTO aiChatRequestDTO) {
        List<BailianClient.BailianMessage> messages = buildMessages(aiChatRequestDTO);

        // SSE 连接不能阻塞 Controller 线程，所以异步调用模型并持续向前端推送分片内容。
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> streamChat(emitter, messages));
        return emitter;
    }

    /**
     * 方法说明：把整理好的消息发送给大模型，并把返回内容持续推送给前端。
     * 主要作用：该方法承担了 AI 模块真正的流式交互工作，让用户可以边生成边看到回答内容。
     * 实现逻辑：实现时会调用百炼客户端进行流式请求，每收到一段文本就通过 SSE 推送 message 事件，完成后发送 done 事件，异常时则发送 error 事件。
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
     * 方法说明：构建发往大模型的完整消息列表。
     * 主要作用：它的作用是把系统角色设定、历史对话和本轮用户输入按模型要求的顺序拼接起来，保证回答风格与上下文连续性。
     * 实现逻辑：方法会先放入系统提示词，再附加经过清洗和裁剪的历史消息，最后追加当前用户消息，形成最终调用模型的 messages 数组。
     */
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

    /**
     * 方法说明：清洗并裁剪前端传入的历史对话记录。
     * 主要作用：该方法用于控制上下文规模，既尽量保留最近对话，又避免把过长历史直接发送给模型造成额外开销。
     * 实现逻辑：实现时会校验角色是否合法、过滤空内容，并仅保留最近限定条数的消息，再转换为模型客户端需要的消息对象。
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

    /**
     * 方法说明：在 小嘉AI对话服务实现 中处理 sendMessageChunk 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：在 小嘉AI对话服务实现 中处理 sendErrorEvent 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
