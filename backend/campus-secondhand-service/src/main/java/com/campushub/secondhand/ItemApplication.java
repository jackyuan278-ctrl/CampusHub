package com.campushub.secondhand;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.campushub.secondhand.mapper")
@SpringBootApplication(scanBasePackages = {"com.campushub.secondhand", "com.campushub.common"})
public class ItemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItemApplication.class, args);
    }
}





