package com.situ.jifeng.rbac.mapper;

import com.situ.jifeng.spi.model.RbacResourceEntity;
import com.situ.jifeng.spi.model.search.RbacResourceSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RbacResourceMapper {
    List<RbacResourceEntity> findAll(RbacResourceSearchBean rse);

    RbacResourceEntity findById(Long id);

    int save(RbacResourceEntity resourceEntity);

    int update(RbacResourceEntity resourceEntity);

    int deleteByIds(List<Long> ids);

    /**
     * 权限判定：返回某后台用户通过 用户组→角色→权限 可访问的接口资源值集合（type=接口）。
     * 判定链路：user → 用户组 → 角色 → 权限 → 资源。
     */
    List<String> findInterfacePathsByUserId(Long userId);
}
