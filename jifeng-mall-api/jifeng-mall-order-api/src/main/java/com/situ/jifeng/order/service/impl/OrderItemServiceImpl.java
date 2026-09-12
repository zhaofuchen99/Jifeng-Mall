package com.situ.jifeng.order.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.OrderItemEntity;
import com.situ.jifeng.spi.model.search.OrderItemSearchBean;
import com.situ.jifeng.spi.service.OrderItemService;
import com.situ.jifeng.order.mapper.OrderItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderItemServiceImpl implements OrderItemService {
    private OrderItemMapper orderItemMapper;

    @Autowired
    public void setOrderItemMapper(OrderItemMapper orderItemMapper) {
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    public List<OrderItemEntity> findAll(OrderItemSearchBean oie, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return orderItemMapper.findAll(oie);
        }
    }

    @Override
    public OrderItemEntity findById(Long id) {
        return orderItemMapper.findById(id);
    }

    @Override
    public List<OrderItemEntity> findByOrderId(Long orderId) {
        return orderItemMapper.findByOrderId(orderId);
    }

    @Override
    public boolean save(OrderItemEntity orderItemEntity) {
        return orderItemMapper.save(orderItemEntity) > 0;
    }

    @Override
    public boolean update(OrderItemEntity orderItemEntity) {
        return orderItemMapper.update(orderItemEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return orderItemMapper.deleteByIds(ids);
    }
}