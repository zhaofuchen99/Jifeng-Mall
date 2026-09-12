package com.situ.jifeng.user;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.situ.jifeng")
public class JifengMallUserApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(JifengMallUserApiApplication.class, args);
    }
}