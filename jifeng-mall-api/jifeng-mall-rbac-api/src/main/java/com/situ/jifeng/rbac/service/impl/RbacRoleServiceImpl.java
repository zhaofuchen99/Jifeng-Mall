package com.situ.jifeng.rbac.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacRoleEntity;
import com.situ.jifeng.spi.model.search.RbacRoleSearchBean;
import com.situ.jifeng.spi.service.RbacRoleService;
import com.situ.jifeng.rbac.mapper.RbacRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RbacRoleServiceImpl implements RbacRoleService {
    private RbacRoleMapper rbacRoleMapper;

    @Autowired
    public void setRbacRoleMapper(RbacRoleMapper rbacRoleMapper) {
        this.rbacRoleMapper = rbacRoleMapper;
    }

    @Override
    public List<RbacRoleEntity> findAll(RbacRoleSearchBean re, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return rbacRoleMapper.findAll(re);
        }
    }

    @Override
    public RbacRoleEntity findById(Long id) {
        return rbacRoleMapper.findById(id);
    }

    @Override
    public boolean save(RbacRoleEntity roleEntity) {
        return rbacRoleMapper.save(roleEntity) > 0;
    }

    @Override
    public boolean update(RbacRoleEntity roleEntity) {
        return rbacRoleMapper.update(roleEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return rbacRoleMapper.deleteByIds(ids);
    }
}
