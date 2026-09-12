package com.situ.jifeng.seckill.service;

import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.spi.model.OrderEntity;
import com.situ.jifeng.spi.model.SeckillOrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "jifeng-mall-order-api", contextId = "seckill-order")
public interface OrderFeignService {

    @PostMapping("/api/orders/seckill")
    TypedJsonResp<OrderEntity> createSeckillOrder(@RequestBody SeckillOrderDTO dto);

    /** 按秒杀流水号查订单（幂等判断），不存在时 data 为 null */
    @GetMapping("/api/orders/seckill-no/{seckillNo}")
    TypedJsonResp<OrderEntity> findBySeckillNo(@PathVariable("seckillNo") String seckillNo);
}
