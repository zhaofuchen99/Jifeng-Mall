package com.situ.jifeng.rbac.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacPermEntity;
import com.situ.jifeng.spi.model.search.RbacPermSearchBean;
import com.situ.jifeng.spi.service.RbacPermService;
import com.situ.jifeng.rbac.mapper.RbacPermMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RbacPermServiceImpl implements RbacPermService {
    private RbacPermMapper rbacPermMapper;

    @Autowired
    public void setRbacPermMapper(RbacPermMapper rbacPermMapper) {
        this.rbacPermMapper = rbacPermMapper;
    }

    @Override
    public List<RbacPermEntity> findAll(RbacPermSearchBean pe, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return rbacPermMapper.findAll(pe);
        }
    }

    @Override
    public RbacPermEntity findById(Long id) {
        return rbacPermMapper.findById(id);
    }

    @Override
    public boolean save(RbacPermEntity permEntity) {
        return rbacPermMapper.save(permEntity) > 0;
    }

    @Override
    public boolean update(RbacPermEntity permEntity) {
        return rbacPermMapper.update(permEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return rbacPermMapper.deleteByIds(ids);
    }
}
