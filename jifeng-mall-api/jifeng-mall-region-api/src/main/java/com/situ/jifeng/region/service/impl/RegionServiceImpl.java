package com.situ.jifeng.region.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RegionEntity;
import com.situ.jifeng.spi.model.search.RegionSearchBean;
import com.situ.jifeng.spi.service.RegionService;
import com.situ.jifeng.region.mapper.RegionMapper;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@CacheConfig(cacheNames = "c.s.s.region.service.impl.RegionServiceImpl")
@Service
public class RegionServiceImpl implements RegionService {
    private RegionMapper regionMapper;

    @Autowired
    public void setRegionMapper(RegionMapper regionMapper) {
        this.regionMapper = regionMapper;
    }

    @Override
    public List<RegionEntity> findAll(RegionSearchBean re, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            List<RegionEntity> regions = regionMapper.findAll(re);
            regions.forEach(this::makeFull);
            return regions;
        }
    }

    @Cacheable(key = "'findByParentId-'+#a0")
    @Override
    public List<RegionEntity> findByParentId(Long parentId) {
        RegionSearchBean re = new RegionSearchBean();

        if (parentId == null || parentId <= 0) {
            re.setLevel(1);
        } else {
            re.setParentId(parentId);
        }
        return findAll(re, PaginateInfo.from(1, 0));
    }

    @Cacheable(key = "'findById-'+#id")
    @Override
    public RegionEntity findById(Long id) {
        RegionEntity re = regionMapper.findById(id);
        if (re != null) {
            makeFull(re);
        }
        return re;
    }

    //递归查询父类别
    private void makeFull(RegionEntity region) {
        if (region.getParentId() != null && region.getParent() == null) {
            RegionEntity parent = self().findById(region.getParentId());
            if (parent != null) {
                region.setParent(parent);
                makeFull(parent);
            }
        }
    }

    private RegionService self() {
        return (RegionService) AopContext.currentProxy();
    }
}