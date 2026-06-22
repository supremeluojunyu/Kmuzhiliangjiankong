package com.uqm;

import com.uqm.config.TimeZoneConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
@MapperScan("com.uqm.mapper")
@org.springframework.scheduling.annotation.EnableScheduling
public class UqmApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(TimeZoneConfig.ZONE_ID));
        SpringApplication.run(UqmApplication.class, args);
    }
}
