package com.situ.jifeng.category;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.situ.jifeng")
public class JifengMallCategoryApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(JifengMallCategoryApiApplication.class, args);
    }
}
