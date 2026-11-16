package com.mpq;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // 启用异步支持
@EnableCaching //启用定时任务
@EnableRabbit
public class TravelGuidePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelGuidePlatformApplication.class, args);
    }
}


