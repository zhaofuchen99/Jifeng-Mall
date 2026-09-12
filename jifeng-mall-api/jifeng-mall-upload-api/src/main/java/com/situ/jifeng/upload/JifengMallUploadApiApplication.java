package com.situ.jifeng.upload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.situ.jifeng")
public class JifengMallUploadApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JifengMallUploadApiApplication.class, args);
    }

}
