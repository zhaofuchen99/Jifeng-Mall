package com.situ.jifeng.brand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.situ.jifeng")
public class JifengMallBrandApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(JifengMallBrandApiApplication.class, args);
    }
}
