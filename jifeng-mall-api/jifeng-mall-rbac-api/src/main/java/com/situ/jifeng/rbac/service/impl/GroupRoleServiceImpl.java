package com.situ.jifeng.rbac.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.GroupRoleEntity;
import com.situ.jifeng.spi.model.search.GroupRoleSearchBean;
import com.situ.jifeng.spi.service.GroupRoleService;
import com.situ.jifeng.rbac.mapper.GroupRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupRoleServiceImpl implements GroupRoleService {
    private GroupRoleMapper groupRoleMapper;

    @Autowired
    public void setGroupRoleMapper(GroupRoleMapper groupRoleMapper) {
        this.groupRoleMapper = groupRoleMapper;
    }

    @Override
    public List<GroupRoleEntity> findAll(GroupRoleSearchBean gre, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return groupRoleMapper.findAll(gre);
        }
    }

    @Override
    public GroupRoleEntity findById(Long id) {
        return groupRoleMapper.findById(id);
    }

    @Override
    public List<GroupRoleEntity> findByGroupId(Long groupId) {
        return groupRoleMapper.findByGroupId(groupId);
    }

    @Override
    public boolean save(GroupRoleEntity groupRoleEntity) {
        return groupRoleMapper.save(groupRoleEntity) > 0;
    }

    @Override
    public boolean update(GroupRoleEntity groupRoleEntity) {
        return groupRoleMapper.update(groupRoleEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return groupRoleMapper.deleteByIds(ids);
    }
}
