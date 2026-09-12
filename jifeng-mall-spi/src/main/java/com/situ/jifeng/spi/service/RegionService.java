package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RegionEntity;
import com.situ.jifeng.spi.model.search.RegionSearchBean;

import java.util.List;

public interface RegionService {
    List<RegionEntity> findAll(RegionSearchBean re, PaginateInfo pi);

    List<RegionEntity> findByParentId(Long parentId);

    RegionEntity findById(Long id);
}