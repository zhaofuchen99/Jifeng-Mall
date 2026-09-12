package com.situ.jifeng.order.service;

import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.spi.model.RestockParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "jifeng-mall-seckill-api", contextId = "order-seckill")
public interface SeckillFeignService {

    @PostMapping("/api/seckills/restock")
    JsonResp restock(@RequestBody RestockParam param);
}
