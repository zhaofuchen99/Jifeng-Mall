package com.situ.jifeng.order.service;

import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.spi.model.MemberAddressEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "jifeng-mall-member-address-api", contextId = "order-address")
public interface AddressFeignService {

    @GetMapping("/api/member-addresses/id/{id}")
    TypedJsonResp<MemberAddressEntity> findById(@PathVariable Long id);
}
