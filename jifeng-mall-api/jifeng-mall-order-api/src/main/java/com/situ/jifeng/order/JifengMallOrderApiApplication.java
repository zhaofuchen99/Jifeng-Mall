package com.situ.jifeng.order;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients
@EnableDiscoveryClient
// 超时关单的兜底扫描 OrderTimeoutCloseTask 依赖定时调度
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.situ.jifeng")
public class JifengMallOrderApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(JifengMallOrderApiApplication.class, args);
    }
}