package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RolePermEntity;
import com.situ.jifeng.spi.model.search.RolePermSearchBean;

import java.util.List;

public interface RolePermService {
    List<RolePermEntity> findAll(RolePermSearchBean rpe, PaginateInfo pi);

    RolePermEntity findById(Long id);

    List<RolePermEntity> findByRoleId(Long roleId);

    boolean save(RolePermEntity rolePermEntity);

    boolean update(RolePermEntity rolePermEntity);

    int deleteByIds(List<Long> ids);
}