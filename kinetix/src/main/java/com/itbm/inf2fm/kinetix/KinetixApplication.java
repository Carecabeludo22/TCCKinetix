package com.itbm.inf2fm.kinetix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KinetixApplication {
    public static void main(String[] args) {
        SpringApplication.run(KinetixApplication.class, args);
    }
}
