package com.homechef.homechefsystem.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.Protocol;
import com.aliyun.oss.common.comm.SignVersion;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class OssConfig {

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(OssProperties ossProperties) {
        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        configuration.setProtocol(Protocol.HTTPS);
        configuration.setSignatureVersion(SignVersion.V4);

        return OSSClientBuilder.create()
                .endpoint(ossProperties.getEndpoint())
                .region(ossProperties.getRegion())
                .credentialsProvider(new DefaultCredentialProvider(
                        ossProperties.getAccessKeyId(),
                        ossProperties.getAccessKeySecret()
                ))
                .clientConfiguration(configuration)
                .build();
    }
}
