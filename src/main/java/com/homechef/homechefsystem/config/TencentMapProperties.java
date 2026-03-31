package com.homechef.homechefsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "tencent.map")
public class TencentMapProperties {

    private boolean enabled;

    private String key;

    private String baseUrl = "https://apis.map.qq.com";

    private String distanceMatrixPath = "/ws/distance/v1/matrix";

    private String mode = "driving";

    private int connectTimeoutMs = 2000;

    private int readTimeoutMs = 3000;
}
