package com.situ.jifeng.brand.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.BrandEntity;
import com.situ.jifeng.spi.model.search.BrandSearchBean;
import com.situ.jifeng.spi.service.BrandService;
import com.situ.jifeng.brand.mapper.BrandMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@CacheConfig(cacheNames = "c.s.s.brand.service.impl.BrandServiceImpl")
@Service
public class BrandServiceImpl implements BrandService {
    private BrandMapper brandMapper;

    @Autowired
    public void setBrandMapper(BrandMapper brandMapper) {
        this.brandMapper = brandMapper;
    }

    @Override
    public List<BrandEntity> findAll(BrandSearchBean be, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return brandMapper.findAll(be);
        }
    }

    @Cacheable(key = "'findById-'+#id")
    @Override
    public BrandEntity findById(Long id) {
        return brandMapper.findById(id);
    }

    @CacheEvict(allEntries = true)
    @Override
    public boolean save(BrandEntity brandEntity) {
        return brandMapper.save(brandEntity) > 0;
    }

    @CacheEvict(allEntries = true)
    @Override
    public boolean update(BrandEntity brandEntity) {
        return brandMapper.update(brandEntity) > 0;
    }

    @CacheEvict(allEntries = true)
    @Override
    public int deleteByIds(List<Long> ids) {
        return brandMapper.deleteByIds(ids);
    }
}
