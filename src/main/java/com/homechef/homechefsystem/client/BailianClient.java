package com.homechef.homechefsystem.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.config.BailianProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class BailianClient {

    private final RestTemplate restTemplate;
    private final BailianProperties bailianProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String chat(List<BailianMessage> messages) {
        validateConfig();

        HttpHeaders headers = buildJsonHeaders();
        BailianChatRequest request = BailianChatRequest.builder()
                .model(bailianProperties.getModel())
                .stream(false)
                .messages(messages)
                .build();

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    buildChatUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    JsonNode.class
            );
            JsonNode body = response.getBody();
            if (body == null) {
                throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "bailian response is empty");
            }

            JsonNode errorNode = body.path("error");
            if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                throw new BusinessException(
                        ResultCodeEnum.SYSTEM_ERROR,
                        "bailian request failed: " + extractErrorMessage(errorNode)
                );
            }

            JsonNode contentNode = body.path("choices").path(0).path("message").path("content");
            String content = contentNode.isMissingNode() || contentNode.isNull() ? null : contentNode.asText();
            if (!StringUtils.hasText(content)) {
                throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "bailian reply is empty");
            }
            return content.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            throw buildHttpException(e);
        } catch (RestClientException e) {
            log.error("bailian request failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "bailian request failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("bailian response parse failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "failed to parse bailian response");
        }
    }

    public void streamChat(List<BailianMessage> messages, Consumer<String> chunkConsumer) {
        validateConfig();

        HttpHeaders headers = buildJsonHeaders();
        headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON));

        BailianChatRequest request = BailianChatRequest.builder()
                .model(bailianProperties.getModel())
                .stream(true)
                .messages(messages)
                .build();

        try {
            restTemplate.execute(
                    buildChatUrl(),
                    HttpMethod.POST,
                    clientHttpRequest -> {
                        clientHttpRequest.getHeaders().putAll(headers);
                        objectMapper.writeValue(clientHttpRequest.getBody(), request);
                    },
                    clientHttpResponse -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(clientHttpResponse.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                handleStreamLine(line, chunkConsumer);
                            }
                        } catch (IOException e) {
                            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "failed to read bailian stream");
                        }
                        return null;
                    }
            );
        } catch (BusinessException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            throw buildHttpException(e);
        } catch (RestClientException e) {
            log.error("bailian stream request failed: {}", e.getMessage(), e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "bailian request failed: " + e.getMessage());
        }
    }

    private HttpHeaders buildJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(bailianProperties.getApiKey());
        return headers;
    }

    private void validateConfig() {
        if (!StringUtils.hasText(bailianProperties.getApiKey())
                || !StringUtils.hasText(bailianProperties.getBaseUrl())
                || !StringUtils.hasText(bailianProperties.getModel())) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "bailian config is missing");
        }
        if ("YOUR_BAILIAN_API_KEY".equals(bailianProperties.getApiKey())) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "bailian api key is not configured");
        }
    }

    private String buildChatUrl() {
        String baseUrl = bailianProperties.getBaseUrl().trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/chat/completions";
    }

    private void handleStreamLine(String line, Consumer<String> chunkConsumer) {
        if (!StringUtils.hasText(line)) {
            return;
        }
        String trimmedLine = line.trim();
        if (!trimmedLine.startsWith("data:")) {
            return;
        }

        String data = trimmedLine.substring(5).trim();
        if (!StringUtils.hasText(data) || "[DONE]".equals(data)) {
            return;
        }

        try {
            JsonNode body = objectMapper.readTree(data);
            JsonNode errorNode = body.path("error");
            if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                throw new BusinessException(
                        ResultCodeEnum.SYSTEM_ERROR,
                        "bailian request failed: " + extractErrorMessage(errorNode)
                );
            }

            JsonNode deltaContentNode = body.path("choices").path(0).path("delta").path("content");
            if (deltaContentNode.isMissingNode() || deltaContentNode.isNull()) {
                return;
            }

            String content = deltaContentNode.asText();
            if (StringUtils.hasText(content)) {
                chunkConsumer.accept(content);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("failed to parse bailian stream chunk: {}", data, e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "failed to parse bailian stream response");
        }
    }

    private BusinessException buildHttpException(HttpStatusCodeException e) {
        String responseBody = e.getResponseBodyAsString();
        String errorMessage = extractErrorMessage(responseBody);
        log.error("bailian http request failed, status={}, body={}", e.getStatusCode(), responseBody, e);
        return new BusinessException(
                ResultCodeEnum.SYSTEM_ERROR,
                "bailian request failed: HTTP " + e.getStatusCode().value() + " " + errorMessage
        );
    }

    private String extractErrorMessage(JsonNode errorNode) {
        if (errorNode == null || errorNode.isMissingNode() || errorNode.isNull()) {
            return "unknown error";
        }
        String message = errorNode.path("message").asText(null);
        if (StringUtils.hasText(message)) {
            return message;
        }
        String code = errorNode.path("code").asText(null);
        if (StringUtils.hasText(code)) {
            return code;
        }
        return "unknown error";
    }

    private String extractErrorMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "empty response";
        }
        try {
            JsonNode body = objectMapper.readTree(responseBody);
            JsonNode errorNode = body.path("error");
            if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                return extractErrorMessage(errorNode);
            }
            String message = body.path("message").asText(null);
            if (StringUtils.hasText(message)) {
                return message;
            }
        } catch (Exception ignored) {
        }
        return responseBody;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BailianChatRequest {

        private String model;

        private List<BailianMessage> messages;

        private Boolean stream;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BailianMessage {

        private String role;

        private String content;
    }
}
