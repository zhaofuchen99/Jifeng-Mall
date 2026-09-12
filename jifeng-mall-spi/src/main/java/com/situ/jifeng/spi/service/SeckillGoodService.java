package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.SeckillGoodEntity;
import com.situ.jifeng.spi.model.search.SeckillGoodSearchBean;

import java.util.List;

public interface SeckillGoodService {
    List<SeckillGoodEntity> findAll(SeckillGoodSearchBean sgs, PaginateInfo pi);

    SeckillGoodEntity findById(Long id);

    boolean save(SeckillGoodEntity seckillGoodEntity);

    boolean update(SeckillGoodEntity seckillGoodEntity);

    int deleteByIds(List<Long> ids);

    List<SeckillGoodEntity> findBySeckillId(Long seckillId);
}
