package com.situ.jifeng.order.service;

import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.spi.model.CartItemEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(value = "jifeng-mall-cart-api", contextId = "order-cart")
public interface CartFeignService {

    @GetMapping("/api/carts/id/{id}")
    TypedJsonResp<CartItemEntity> findById(@PathVariable Long id);

    @GetMapping("/api/carts/member/{memberId}")
    TypedJsonResp<List<CartItemEntity>> findByMemberId(@PathVariable Long memberId);

    @DeleteMapping("/api/carts")
    JsonResp deleteByIds(@RequestBody Long[] ids);
}
