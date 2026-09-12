package com.situ.jifeng.order.mapper;

import com.situ.jifeng.spi.model.OrderItemEntity;
import com.situ.jifeng.spi.model.search.OrderItemSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderItemMapper {
    List<OrderItemEntity> findAll(OrderItemSearchBean oie);

    OrderItemEntity findById(Long id);

    List<OrderItemEntity> findByOrderId(Long orderId);

    int save(OrderItemEntity orderItemEntity);

    int update(OrderItemEntity orderItemEntity);

    int deleteByIds(List<Long> ids);
}