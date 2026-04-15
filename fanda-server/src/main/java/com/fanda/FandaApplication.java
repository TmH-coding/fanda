package com.fanda;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.fanda.mapper")
public class FandaApplication {
    public static void main(String[] args) {
        SpringApplication.run(FandaApplication.class, args);
    }
}
