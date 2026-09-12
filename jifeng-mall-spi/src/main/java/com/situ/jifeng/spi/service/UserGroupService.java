package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.UserGroupEntity;
import com.situ.jifeng.spi.model.search.UserGroupSearchBean;

import java.util.List;

public interface UserGroupService {
    List<UserGroupEntity> findAll(UserGroupSearchBean uge, PaginateInfo pi);

    UserGroupEntity findById(Long id);

    List<UserGroupEntity> findByGroupId(Long groupId);

    List<UserGroupEntity> findByUserId(Long userId);

    boolean save(UserGroupEntity userGroupEntity);

    boolean update(UserGroupEntity userGroupEntity);

    int deleteByIds(List<Long> ids);
}