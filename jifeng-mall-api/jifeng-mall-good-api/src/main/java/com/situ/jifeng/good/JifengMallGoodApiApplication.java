package com.situ.jifeng.good;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.situ.jifeng")
public class JifengMallGoodApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JifengMallGoodApiApplication.class, args);
    }

}
