package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacRoleEntity;
import com.situ.jifeng.spi.model.search.RbacRoleSearchBean;

import java.util.List;

public interface RbacRoleService {
    List<RbacRoleEntity> findAll(RbacRoleSearchBean re, PaginateInfo pi);

    RbacRoleEntity findById(Long id);

    boolean save(RbacRoleEntity roleEntity);

    boolean update(RbacRoleEntity roleEntity);

    int deleteByIds(List<Long> ids);
}