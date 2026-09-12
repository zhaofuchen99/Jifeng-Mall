package com.situ.jifeng.rbac.mapper;

import com.situ.jifeng.spi.model.RbacPermEntity;
import com.situ.jifeng.spi.model.search.RbacPermSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RbacPermMapper {
    List<RbacPermEntity> findAll(RbacPermSearchBean pe);

    RbacPermEntity findById(Long id);

    int save(RbacPermEntity permEntity);

    int update(RbacPermEntity permEntity);

    int deleteByIds(List<Long> ids);
}
