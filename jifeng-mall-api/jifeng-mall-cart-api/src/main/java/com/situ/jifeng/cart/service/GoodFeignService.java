package com.situ.jifeng.cart.service;

import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.spi.model.GoodEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "jifeng-mall-good-api", contextId = "cart-api")
public interface GoodFeignService {

    @GetMapping("/api/goods/id/{id}")
    TypedJsonResp<GoodEntity> findById(@PathVariable Long id);
}
