package com.situ.jifeng.rbac;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.situ.jifeng")
public class JifengMallRbacApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(JifengMallRbacApiApplication.class, args);
    }
}