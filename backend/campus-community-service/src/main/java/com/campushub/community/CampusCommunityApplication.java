package com.campushub.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.campushub.community.mapper")
@SpringBootApplication(scanBasePackages = {"com.campushub.community", "com.campushub.common"})
@EnableScheduling
public class CampusCommunityApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusCommunityApplication.class, args);
    }
}







