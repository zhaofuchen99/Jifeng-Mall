package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.PermResourceEntity;
import com.situ.jifeng.spi.model.search.PermResourceSearchBean;

import java.util.List;

public interface PermResourceService {
    List<PermResourceEntity> findAll(PermResourceSearchBean pre, PaginateInfo pi);

    PermResourceEntity findById(Long id);

    List<PermResourceEntity> findByPermId(Long permId);

    List<PermResourceEntity> findByResourceId(Long resourceId);

    boolean save(PermResourceEntity permResourceEntity);

    boolean update(PermResourceEntity permResourceEntity);

    int deleteByIds(List<Long> ids);
}