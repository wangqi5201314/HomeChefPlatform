package com.homechef.homechefsystem.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TencentMapProperties.class)
public class TencentMapConfig {
}
