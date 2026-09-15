package com.situ.jifeng.category;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
// 删除分类前要调 good-api 校验商品引用（GoodFeignService）
@EnableFeignClients
@SpringBootApplication(scanBasePackages = "com.situ.jifeng")
public class JifengMallCategoryApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(JifengMallCategoryApiApplication.class, args);
    }
}
