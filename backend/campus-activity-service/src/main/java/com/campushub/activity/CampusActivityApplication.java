package com.campushub.activity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.campushub.activity.mapper")
@SpringBootApplication(scanBasePackages = {"com.campushub.activity", "com.campushub.common"})
@EnableScheduling
public class CampusActivityApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusActivityApplication.class, args);
    }
}







