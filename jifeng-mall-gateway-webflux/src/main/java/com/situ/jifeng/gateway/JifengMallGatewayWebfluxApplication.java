package com.situ.jifeng.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class JifengMallGatewayWebfluxApplication {

    static void main(String[] args) {
        SpringApplication.run(JifengMallGatewayWebfluxApplication.class, args);
    }
}
