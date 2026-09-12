package com.situ.jifeng.rbac.mapper;

import com.situ.jifeng.spi.model.PermResourceEntity;
import com.situ.jifeng.spi.model.search.PermResourceSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PermResourceMapper {
    List<PermResourceEntity> findAll(PermResourceSearchBean pre);

    PermResourceEntity findById(Long id);

    List<PermResourceEntity> findByPermId(Long permId);

    List<PermResourceEntity> findByResourceId(Long resourceId);

    int save(PermResourceEntity permResourceEntity);

    int update(PermResourceEntity permResourceEntity);

    int deleteByIds(List<Long> ids);
}
