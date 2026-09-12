package com.situ.jifeng.rbac.mapper;

import com.situ.jifeng.spi.model.RolePermEntity;
import com.situ.jifeng.spi.model.search.RolePermSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RolePermMapper {
    List<RolePermEntity> findAll(RolePermSearchBean rpe);

    RolePermEntity findById(Long id);

    List<RolePermEntity> findByRoleId(Long roleId);

    int save(RolePermEntity rolePermEntity);

    int update(RolePermEntity rolePermEntity);

    int deleteByIds(List<Long> ids);
}
