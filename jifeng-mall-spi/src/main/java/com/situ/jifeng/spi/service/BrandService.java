package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.BrandEntity;
import com.situ.jifeng.spi.model.search.BrandSearchBean;

import java.util.List;

public interface BrandService {
    List<BrandEntity> findAll(BrandSearchBean be, PaginateInfo pi);

    BrandEntity findById(Long id);

    boolean save(BrandEntity brandEntity);

    boolean update(BrandEntity brandEntity);

    int deleteByIds(List<Long> ids);
}
