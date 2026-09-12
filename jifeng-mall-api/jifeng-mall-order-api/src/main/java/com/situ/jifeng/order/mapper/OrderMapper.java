package com.situ.jifeng.order.mapper;

import com.situ.jifeng.spi.model.OrderEntity;
import com.situ.jifeng.spi.model.search.OrderSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderMapper {
    List<OrderEntity> findAll(OrderSearchBean osb);

    OrderEntity findById(Long id);

    OrderEntity findByOrderNo(String orderNo);

    OrderEntity findBySeckillNo(String seckillNo);

    int save(OrderEntity orderEntity);

    int update(OrderEntity orderEntity);

    int deleteByIds(List<Long> ids);
}