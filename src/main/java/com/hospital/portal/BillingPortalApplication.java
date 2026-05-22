package com.hospital.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class BillingPortalApplication {
    public static void main(String[] args) {
        SpringApplication.run(BillingPortalApplication.class, args);
    }
}
