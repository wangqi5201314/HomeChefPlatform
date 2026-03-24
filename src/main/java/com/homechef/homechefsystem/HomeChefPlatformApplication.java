package com.homechef.homechefsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HomeChefPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeChefPlatformApplication.class, args);
    }

}
