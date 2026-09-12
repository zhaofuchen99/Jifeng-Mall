package com.situ.jifeng.rbac.mapper;

import com.situ.jifeng.spi.model.RbacRoleEntity;
import com.situ.jifeng.spi.model.search.RbacRoleSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RbacRoleMapper {
    List<RbacRoleEntity> findAll(RbacRoleSearchBean re);

    RbacRoleEntity findById(Long id);

    int save(RbacRoleEntity roleEntity);

    int update(RbacRoleEntity roleEntity);

    int deleteByIds(List<Long> ids);
}
