package com.situ.jifeng.rbac.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.PermResourceEntity;
import com.situ.jifeng.spi.model.search.PermResourceSearchBean;
import com.situ.jifeng.spi.service.PermResourceService;
import com.situ.jifeng.rbac.mapper.PermResourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermResourceServiceImpl implements PermResourceService {
    private PermResourceMapper permResourceMapper;

    @Autowired
    public void setPermResourceMapper(PermResourceMapper permResourceMapper) {
        this.permResourceMapper = permResourceMapper;
    }

    @Override
    public List<PermResourceEntity> findAll(PermResourceSearchBean pre, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return permResourceMapper.findAll(pre);
        }
    }

    @Override
    public PermResourceEntity findById(Long id) {
        return permResourceMapper.findById(id);
    }

    @Override
    public List<PermResourceEntity> findByPermId(Long permId) {
        return permResourceMapper.findByPermId(permId);
    }

    @Override
    public List<PermResourceEntity> findByResourceId(Long resourceId) {
        return permResourceMapper.findByResourceId(resourceId);
    }

    @Override
    public boolean save(PermResourceEntity permResourceEntity) {
        return permResourceMapper.save(permResourceEntity) > 0;
    }

    @Override
    public boolean update(PermResourceEntity permResourceEntity) {
        return permResourceMapper.update(permResourceEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return permResourceMapper.deleteByIds(ids);
    }
}
