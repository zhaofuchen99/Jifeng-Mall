package com.situ.jifeng.rbac.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacCheckParam;
import com.situ.jifeng.spi.model.RbacGrant;
import com.situ.jifeng.spi.model.RbacResourceEntity;
import com.situ.jifeng.spi.model.search.RbacResourceSearchBean;
import com.situ.jifeng.spi.service.RbacResourceService;
import com.situ.jifeng.rbac.mapper.RbacResourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Service
public class RbacResourceServiceImpl implements RbacResourceService {
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private RbacResourceMapper rbacResourceMapper;

    @Autowired
    public void setRbacResourceMapper(RbacResourceMapper rbacResourceMapper) {
        this.rbacResourceMapper = rbacResourceMapper;
    }

    @Override
    public List<RbacResourceEntity> findAll(RbacResourceSearchBean rse, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return rbacResourceMapper.findAll(rse);
        }
    }

    @Override
    public RbacResourceEntity findById(Long id) {
        return rbacResourceMapper.findById(id);
    }

    @Override
    public boolean save(RbacResourceEntity resourceEntity) {
        return rbacResourceMapper.save(resourceEntity) > 0;
    }

    @Override
    public boolean update(RbacResourceEntity resourceEntity) {
        return rbacResourceMapper.update(resourceEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return rbacResourceMapper.deleteByIds(ids);
    }

    @Override
    public RbacGrant check(RbacCheckParam param) {
        if (param.getUserId() == null || param.getPath() == null) {
            return RbacGrant.builder().allowed(false).build();
        }
        List<String> paths = rbacResourceMapper.findInterfacePathsByUserId(param.getUserId());
        if (paths == null || paths.isEmpty()) {
            return RbacGrant.builder().allowed(false).build();
        }
        // path 匹配资源 value（Ant 风格），method 作为补充（资源默认基于路径）
        for (String p : paths) {
            if (p != null && MATCHER.match(p, param.getPath())) {
                return RbacGrant.builder().allowed(true).resource(p).build();
            }
        }
        return RbacGrant.builder().allowed(false).build();
    }
}
