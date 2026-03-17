package com.zym.hd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HdApplication {

    public static void main(String[] args) {
        SpringApplication.run(HdApplication.class, args);
    }

}

