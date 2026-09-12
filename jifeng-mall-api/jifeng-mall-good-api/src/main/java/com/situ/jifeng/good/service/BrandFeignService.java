package com.situ.jifeng.good.service;

import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.spi.model.BrandEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "jifeng-mall-brand-api", contextId = "brand-api", fallback = BrandFeignServiceFallback.class)
public interface BrandFeignService {
    @GetMapping("/api/brands/id/{id}")
    TypedJsonResp<BrandEntity> findById(@PathVariable Long id);
}

//降级
@Component
class BrandFeignServiceFallback implements BrandFeignService {
    @Override
    public TypedJsonResp<BrandEntity> findById(Long id) {
        return TypedJsonResp.fail(503, "服务不可用");
    }
}