package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacPermEntity;
import com.situ.jifeng.spi.model.search.RbacPermSearchBean;

import java.util.List;

public interface RbacPermService {
    List<RbacPermEntity> findAll(RbacPermSearchBean pe, PaginateInfo pi);

    RbacPermEntity findById(Long id);

    boolean save(RbacPermEntity permEntity);

    boolean update(RbacPermEntity permEntity);

    int deleteByIds(List<Long> ids);
}