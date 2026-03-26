package com.homechef.homechefsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bailian")
public class BailianProperties {

    private String apiKey;

    private String baseUrl;

    private String model;
}
