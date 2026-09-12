package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.OrderItemEntity;
import com.situ.jifeng.spi.model.search.OrderItemSearchBean;

import java.util.List;

public interface OrderItemService {
    List<OrderItemEntity> findAll(OrderItemSearchBean oie, PaginateInfo pi);

    OrderItemEntity findById(Long id);

    List<OrderItemEntity> findByOrderId(Long orderId);

    boolean save(OrderItemEntity orderItemEntity);

    boolean update(OrderItemEntity orderItemEntity);

    int deleteByIds(List<Long> ids);
}