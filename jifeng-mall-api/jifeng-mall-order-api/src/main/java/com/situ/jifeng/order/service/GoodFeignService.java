package com.situ.jifeng.order.service;

import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.spi.model.DeductStockDto;
import com.situ.jifeng.spi.model.GoodEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "jifeng-mall-good-api", contextId = "order-api")
public interface GoodFeignService {

    @GetMapping("/api/goods/id/{id}")
    TypedJsonResp<GoodEntity> findById(@PathVariable Long id, @RequestParam boolean full);

    @PostMapping("/api/goods/{id}/deduct")
    JsonResp deductStock(@PathVariable Long id, @RequestBody DeductStockDto dto);

    @PostMapping("/api/goods/{id}/add-back")
    JsonResp addBackStock(@PathVariable Long id, @RequestBody DeductStockDto dto);
}
