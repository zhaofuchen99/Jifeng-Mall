package com.situ.jifeng.good.service;

import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.spi.model.CategoryEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "jifeng-mall-category-api", contextId = "category-api")
public interface CategoryFeignService {
    @GetMapping("/api/categories/id/{id}")
    TypedJsonResp<CategoryEntity> findById(@PathVariable Long id);
}
