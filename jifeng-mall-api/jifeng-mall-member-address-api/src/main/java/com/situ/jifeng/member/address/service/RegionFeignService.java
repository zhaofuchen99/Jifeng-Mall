package com.situ.jifeng.member.address.service;

import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.spi.model.RegionEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "jifeng-mall-region-api", contextId = "region-api")
public interface RegionFeignService {

    @GetMapping("/api/regions/id/{id}")
    TypedJsonResp<RegionEntity> findById(@PathVariable Long id);
}
