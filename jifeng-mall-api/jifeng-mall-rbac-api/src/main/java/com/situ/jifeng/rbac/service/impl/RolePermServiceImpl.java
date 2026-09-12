package com.situ.jifeng.rbac.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RolePermEntity;
import com.situ.jifeng.spi.model.search.RolePermSearchBean;
import com.situ.jifeng.spi.service.RolePermService;
import com.situ.jifeng.rbac.mapper.RolePermMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolePermServiceImpl implements RolePermService {
    private RolePermMapper rolePermMapper;

    @Autowired
    public void setRolePermMapper(RolePermMapper rolePermMapper) {
        this.rolePermMapper = rolePermMapper;
    }

    @Override
    public List<RolePermEntity> findAll(RolePermSearchBean rpe, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return rolePermMapper.findAll(rpe);
        }
    }

    @Override
    public RolePermEntity findById(Long id) {
        return rolePermMapper.findById(id);
    }

    @Override
    public List<RolePermEntity> findByRoleId(Long roleId) {
        return rolePermMapper.findByRoleId(roleId);
    }

    @Override
    public boolean save(RolePermEntity rolePermEntity) {
        return rolePermMapper.save(rolePermEntity) > 0;
    }

    @Override
    public boolean update(RolePermEntity rolePermEntity) {
        return rolePermMapper.update(rolePermEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return rolePermMapper.deleteByIds(ids);
    }
}
