package com.homechef.homechefsystem.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.config.WechatMiniProgramProperties;
import com.homechef.homechefsystem.service.WechatMiniProgramService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(WechatMiniProgramProperties.class)
public class WechatMiniProgramServiceImpl implements WechatMiniProgramService {

    private static final String CODE_2_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final WechatMiniProgramProperties wechatMiniProgramProperties;
    private final ObjectMapper objectMapper;
    /**
     * 处理 r es tt em pl at e 相关逻辑。
     */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 方法说明：调用微信官方 code2Session 接口换取小程序用户的 openid 和会话信息。
     * 主要作用：这是微信小程序登录流程的基础能力，后续的免密登录、用户创建和身份绑定都依赖这一结果。
     * 实现逻辑：方法会先拼接带有 appId、secret 和 code 的请求地址，再发起 HTTP 请求并解析返回值；若微信返回错误码或关键字段缺失，则抛出业务异常。
     */
    @Override
    public WechatLoginInfo code2Session(String code) {
        if (!StringUtils.hasText(wechatMiniProgramProperties.getAppId())
                || !StringUtils.hasText(wechatMiniProgramProperties.getAppSecret())) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "wechat mini program config is missing");
        }

        String requestUrl = UriComponentsBuilder.fromHttpUrl(CODE_2_SESSION_URL)
                .queryParam("appid", wechatMiniProgramProperties.getAppId())
                .queryParam("secret", wechatMiniProgramProperties.getAppSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build(true)
                .toUriString();

        String responseBody;
        try {
            responseBody = restTemplate.getForObject(requestUrl, String.class);
        } catch (RestClientException e) {
            log.error("failed to call wechat login api, url={}", maskSecret(requestUrl), e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "failed to call wechat login api: " + e.getMessage());
        }

        if (!StringUtils.hasText(responseBody)) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "empty response from wechat login api");
        }

        Code2SessionResponse response;
        try {
            response = objectMapper.readValue(responseBody, Code2SessionResponse.class);
        } catch (JsonProcessingException e) {
            log.error("failed to parse wechat login response, body={}", responseBody, e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "failed to parse wechat login response");
        }

        if (response.getErrcode() != null && response.getErrcode() != 0) {
            String message = StringUtils.hasText(response.getErrmsg()) ? response.getErrmsg() : "wechat login failed";
            throw new BusinessException(ResultCodeEnum.FAIL, "wechat login failed: " + message);
        }
        if (!StringUtils.hasText(response.getOpenid())) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "wechat login failed: openid is empty");
        }

        return new WechatLoginInfo(response.getOpenid(), response.getSessionKey(), response.getUnionid());
    }

    /**
     * 方法说明：在 微信小程序登录服务实现 中处理 maskSecret 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    private String maskSecret(String requestUrl) {
        return requestUrl.replace(wechatMiniProgramProperties.getAppSecret(), "******");
    }

    @Data
    public static class Code2SessionResponse {

        private String openid;

        @JsonProperty("session_key")
        private String sessionKey;

        private String unionid;

        private Integer errcode;

        private String errmsg;
    }
}
