package com.campushub.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.campushub.community.mapper")
@EnableFeignClients(basePackages = "com.campushub.api.client")
@SpringBootApplication(scanBasePackages = {"com.campushub.community", "com.campushub.common"})
@EnableScheduling
public class CampusCommunityApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusCommunityApplication.class, args);
    }
}







