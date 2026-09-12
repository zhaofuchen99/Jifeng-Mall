package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacGroupEntity;
import com.situ.jifeng.spi.model.search.RbacGroupSearchBean;

import java.util.List;

public interface RbacGroupService {
    List<RbacGroupEntity> findAll(RbacGroupSearchBean ge, PaginateInfo pi);

    RbacGroupEntity findById(Long id);

    boolean save(RbacGroupEntity groupEntity);

    boolean update(RbacGroupEntity groupEntity);

    int deleteByIds(List<Long> ids);
}