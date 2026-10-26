package com.campushub.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("com.campushub.order.mapper")
@EnableFeignClients(basePackages = "com.campushub.api.client")
@SpringBootApplication(scanBasePackages = "com.campushub")
public class TradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeApplication.class, args);
    }
}




