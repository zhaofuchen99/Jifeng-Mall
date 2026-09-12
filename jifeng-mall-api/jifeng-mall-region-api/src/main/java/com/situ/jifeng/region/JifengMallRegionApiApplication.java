package com.situ.jifeng.region;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.situ.jifeng")
public class JifengMallRegionApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(JifengMallRegionApiApplication.class, args);
    }
}