package com.homechef.homechefsystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("私房菜上门服务系统接口文档")
                        .description("HomeChefPlatform API")
                        .version("1.0.0")
                        .contact(new Contact().name("HomeChef")));
    }
}
