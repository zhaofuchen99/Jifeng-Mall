package com.situ.jifeng.rbac.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.UserGroupEntity;
import com.situ.jifeng.spi.model.search.UserGroupSearchBean;
import com.situ.jifeng.spi.service.UserGroupService;
import com.situ.jifeng.rbac.mapper.UserGroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserGroupServiceImpl implements UserGroupService {
    private UserGroupMapper userGroupMapper;

    @Autowired
    public void setUserGroupMapper(UserGroupMapper userGroupMapper) {
        this.userGroupMapper = userGroupMapper;
    }

    @Override
    public List<UserGroupEntity> findAll(UserGroupSearchBean uge, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return userGroupMapper.findAll(uge);
        }
    }

    @Override
    public UserGroupEntity findById(Long id) {
        return userGroupMapper.findById(id);
    }

    @Override
    public List<UserGroupEntity> findByGroupId(Long groupId) {
        return userGroupMapper.findByGroupId(groupId);
    }

    @Override
    public List<UserGroupEntity> findByUserId(Long userId) {
        return userGroupMapper.findByUserId(userId);
    }

    @Override
    public boolean save(UserGroupEntity userGroupEntity) {
        return userGroupMapper.save(userGroupEntity) > 0;
    }

    @Override
    public boolean update(UserGroupEntity userGroupEntity) {
        return userGroupMapper.update(userGroupEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return userGroupMapper.deleteByIds(ids);
    }
}
