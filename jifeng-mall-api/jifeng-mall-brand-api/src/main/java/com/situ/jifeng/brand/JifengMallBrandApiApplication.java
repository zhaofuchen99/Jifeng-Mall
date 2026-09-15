package com.situ.jifeng.brand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
// 删除品牌前要调 good-api 校验商品引用（GoodFeignService）
@EnableFeignClients
@SpringBootApplication(scanBasePackages = "com.situ.jifeng")
public class JifengMallBrandApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(JifengMallBrandApiApplication.class, args);
    }
}
