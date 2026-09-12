package com.situ.jifeng.rbac.mapper;

import com.situ.jifeng.spi.model.RbacGroupEntity;
import com.situ.jifeng.spi.model.search.RbacGroupSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RbacGroupMapper {
    List<RbacGroupEntity> findAll(RbacGroupSearchBean ge);

    RbacGroupEntity findById(Long id);

    int save(RbacGroupEntity groupEntity);

    int update(RbacGroupEntity groupEntity);

    int deleteByIds(List<Long> ids);
}
