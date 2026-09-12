package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacCheckParam;
import com.situ.jifeng.spi.model.RbacGrant;
import com.situ.jifeng.spi.model.RbacResourceEntity;
import com.situ.jifeng.spi.model.search.RbacResourceSearchBean;

import java.util.List;

public interface RbacResourceService {
    List<RbacResourceEntity> findAll(RbacResourceSearchBean rse, PaginateInfo pi);

    RbacResourceEntity findById(Long id);

    boolean save(RbacResourceEntity resourceEntity);

    boolean update(RbacResourceEntity resourceEntity);

    int deleteByIds(List<Long> ids);

    /**
     * 权限判定：给定后台用户 + 请求 path/method，判断是否授权（供网关调用）。
     * 判定链路：userId→用户组→角色→权限→资源(type=接口, value=path+method)。
     *
     * @return 是否放行
     */
    RbacGrant check(RbacCheckParam param);
}