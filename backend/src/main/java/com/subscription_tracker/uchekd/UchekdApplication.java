package com.subscription_tracker.uchekd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UchekdApplication {
    public static void main(String[] args) {
        SpringApplication.run(UchekdApplication.class, args);
    }
}
