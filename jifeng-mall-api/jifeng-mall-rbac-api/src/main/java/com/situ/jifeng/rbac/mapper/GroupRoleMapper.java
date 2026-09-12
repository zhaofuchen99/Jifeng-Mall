package com.situ.jifeng.rbac.mapper;

import com.situ.jifeng.spi.model.GroupRoleEntity;
import com.situ.jifeng.spi.model.search.GroupRoleSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GroupRoleMapper {
    List<GroupRoleEntity> findAll(GroupRoleSearchBean gre);

    GroupRoleEntity findById(Long id);

    List<GroupRoleEntity> findByGroupId(Long groupId);

    int save(GroupRoleEntity groupRoleEntity);

    int update(GroupRoleEntity groupRoleEntity);

    int deleteByIds(List<Long> ids);
}
