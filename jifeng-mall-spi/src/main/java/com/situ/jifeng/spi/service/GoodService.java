package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.GoodEntity;
import com.situ.jifeng.spi.model.search.GoodSearchBean;

import java.util.List;

public interface GoodService {
    List<GoodEntity> findAll(GoodSearchBean ge, PaginateInfo pi);

    GoodEntity findById(Long id);

    boolean save(GoodEntity goodEntity);

    boolean update(GoodEntity goodEntity);

    int deleteByIds(List<Long> ids);

    /**
     * 扣减库存（防超卖，仅库存充足时成功）。
     *
     * @return true 扣减成功
     */
    boolean deductStock(Long id, Integer qty);

    /**
     * 回补库存（取消订单/超时关单时）。
     */
    boolean addBackStock(Long id, Integer qty);
}
