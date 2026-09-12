package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.GroupRoleEntity;
import com.situ.jifeng.spi.model.search.GroupRoleSearchBean;

import java.util.List;

public interface GroupRoleService {
    List<GroupRoleEntity> findAll(GroupRoleSearchBean gre, PaginateInfo pi);

    GroupRoleEntity findById(Long id);

    List<GroupRoleEntity> findByGroupId(Long groupId);

    boolean save(GroupRoleEntity groupRoleEntity);

    boolean update(GroupRoleEntity groupRoleEntity);

    int deleteByIds(List<Long> ids);
}