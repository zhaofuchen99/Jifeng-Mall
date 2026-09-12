package com.situ.jifeng.rbac.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacGroupEntity;
import com.situ.jifeng.spi.model.search.RbacGroupSearchBean;
import com.situ.jifeng.spi.service.RbacGroupService;
import com.situ.jifeng.rbac.mapper.RbacGroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RbacGroupServiceImpl implements RbacGroupService {
    private RbacGroupMapper rbacGroupMapper;

    @Autowired
    public void setRbacGroupMapper(RbacGroupMapper rbacGroupMapper) {
        this.rbacGroupMapper = rbacGroupMapper;
    }

    @Override
    public List<RbacGroupEntity> findAll(RbacGroupSearchBean ge, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return rbacGroupMapper.findAll(ge);
        }
    }

    @Override
    public RbacGroupEntity findById(Long id) {
        return rbacGroupMapper.findById(id);
    }

    @Override
    public boolean save(RbacGroupEntity groupEntity) {
        return rbacGroupMapper.save(groupEntity) > 0;
    }

    @Override
    public boolean update(RbacGroupEntity groupEntity) {
        return rbacGroupMapper.update(groupEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return rbacGroupMapper.deleteByIds(ids);
    }
}
