package com.situ.jifeng.rbac.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacMenuEntity;
import com.situ.jifeng.spi.model.search.RbacMenuSearchBean;
import com.situ.jifeng.spi.service.RbacMenuService;
import com.situ.jifeng.rbac.mapper.RbacMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RbacMenuServiceImpl implements RbacMenuService {
    private RbacMenuMapper rbacMenuMapper;

    @Autowired
    public void setRbacMenuMapper(RbacMenuMapper rbacMenuMapper) {
        this.rbacMenuMapper = rbacMenuMapper;
    }

    @Override
    public List<RbacMenuEntity> findAll(RbacMenuSearchBean me, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return rbacMenuMapper.findAll(me);
        }
    }

    @Override
    public RbacMenuEntity findById(Long id) {
        return rbacMenuMapper.findById(id);
    }

    @Override
    public boolean save(RbacMenuEntity menuEntity) {
        return rbacMenuMapper.save(menuEntity) > 0;
    }

    @Override
    public boolean update(RbacMenuEntity menuEntity) {
        return rbacMenuMapper.update(menuEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return rbacMenuMapper.deleteByIds(ids);
    }

    @Override
    public List<RbacMenuEntity> findMenuTreeByUserId(Long userId) {
        List<RbacMenuEntity> all = rbacMenuMapper.findMenusByUserId(userId);
        if (all == null || all.isEmpty()) {
            return new ArrayList<>();
        }
        // 按 parentId 分组建树，根为 parentId == 0
        Map<Long, List<RbacMenuEntity>> byParent = all.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getParentId() == null ? 0L : m.getParentId(),
                        Collectors.toList()));
        for (RbacMenuEntity m : all) {
            m.setChildren(byParent.getOrDefault(m.getId(), new ArrayList<>()));
        }
        // 返回根菜单（parent=0）
        return byParent.getOrDefault(0L, new ArrayList<>());
    }
}
