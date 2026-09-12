package com.situ.jifeng.rbac.mapper;

import com.situ.jifeng.spi.model.UserGroupEntity;
import com.situ.jifeng.spi.model.search.UserGroupSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserGroupMapper {
    List<UserGroupEntity> findAll(UserGroupSearchBean uge);

    UserGroupEntity findById(Long id);

    List<UserGroupEntity> findByGroupId(Long groupId);

    List<UserGroupEntity> findByUserId(Long userId);

    int save(UserGroupEntity userGroupEntity);

    int update(UserGroupEntity userGroupEntity);

    int deleteByIds(List<Long> ids);
}
