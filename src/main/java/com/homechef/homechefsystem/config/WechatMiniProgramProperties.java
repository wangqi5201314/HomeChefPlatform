package com.homechef.homechefsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wechat.mini-program")
public class WechatMiniProgramProperties {

    private String appId;

    private String appSecret;
}
