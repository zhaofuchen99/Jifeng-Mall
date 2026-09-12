package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacMenuEntity;
import com.situ.jifeng.spi.model.search.RbacMenuSearchBean;

import java.util.List;

public interface RbacMenuService {
    List<RbacMenuEntity> findAll(RbacMenuSearchBean me, PaginateInfo pi);

    RbacMenuEntity findById(Long id);

    boolean save(RbacMenuEntity menuEntity);

    boolean update(RbacMenuEntity menuEntity);

    int deleteByIds(List<Long> ids);

    /**
     * 动态菜单：根据后台用户的角色权限，返回其可见的菜单树。
     * 判定链路：userId→用户组→角色→权限→资源→菜单(resource_id)。
     *
     * @return 菜单树
     */
    List<RbacMenuEntity> findMenuTreeByUserId(Long userId);
}