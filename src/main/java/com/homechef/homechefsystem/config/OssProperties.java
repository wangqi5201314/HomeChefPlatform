package com.homechef.homechefsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    private String bucketName;

    private String endpoint;

    private String region;

    private String accessKeyId;

    private String accessKeySecret;

    private String uploadPrefix;

    private String allowedTypes;

    private Integer maxSizeMb;

    private String customDomain;
}
