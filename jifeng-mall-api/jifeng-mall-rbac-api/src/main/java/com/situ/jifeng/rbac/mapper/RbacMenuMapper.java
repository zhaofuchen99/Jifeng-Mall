package com.situ.jifeng.rbac.mapper;

import com.situ.jifeng.spi.model.RbacMenuEntity;
import com.situ.jifeng.spi.model.search.RbacMenuSearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RbacMenuMapper {
    List<RbacMenuEntity> findAll(RbacMenuSearchBean me);

    RbacMenuEntity findById(Long id);

    int save(RbacMenuEntity menuEntity);

    int update(RbacMenuEntity menuEntity);

    int deleteByIds(List<Long> ids);

    /**
     * 动态菜单：返回某后台用户通过 用户组→角色→权限→资源 关联到的可见菜单集合。
     */
    List<RbacMenuEntity> findMenusByUserId(Long userId);
}
