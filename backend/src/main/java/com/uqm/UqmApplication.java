package com.uqm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.uqm.mapper")
@org.springframework.scheduling.annotation.EnableScheduling
public class UqmApplication {

    public static void main(String[] args) {
        SpringApplication.run(UqmApplication.class, args);
    }
}
