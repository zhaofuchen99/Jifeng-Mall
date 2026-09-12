package com.situ.jifeng.member;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.situ.jifeng")
public class JifengMallMemberApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(JifengMallMemberApiApplication.class, args);
    }
}