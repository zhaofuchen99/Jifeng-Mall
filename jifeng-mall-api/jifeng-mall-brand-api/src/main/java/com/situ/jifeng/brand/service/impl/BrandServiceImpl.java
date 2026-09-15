package com.situ.jifeng.brand.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.BusinessException;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.spi.model.BrandEntity;
import com.situ.jifeng.spi.model.search.BrandSearchBean;
import com.situ.jifeng.spi.service.BrandService;
import com.situ.jifeng.brand.mapper.BrandMapper;
import com.situ.jifeng.brand.service.GoodFeignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@CacheConfig(cacheNames = "c.s.s.brand.service.impl.BrandServiceImpl")
@Service
public class BrandServiceImpl implements BrandService {

    private static final Logger log = LoggerFactory.getLogger(BrandServiceImpl.class);

    private BrandMapper brandMapper;
    private GoodFeignService goodFeignService;

    @Autowired
    public void setBrandMapper(BrandMapper brandMapper) {
        this.brandMapper = brandMapper;
    }

    @Autowired
    public void setGoodFeignService(GoodFeignService goodFeignService) {
        this.goodFeignService = goodFeignService;
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

    /**
     * 批量删除品牌。<b>删除前逐个校验商品引用</b>（设计文档 5.3「删除校验商品引用」）。
     *
     * <p>不做校验的后果：品牌被删掉后，引用它的商品还在，但
     * {@code GET /api/goods?full=true} 组装品牌时得到 null —— 商品列表里品牌一栏凭空消失，
     * 且没有任何报错，属于典型的静默数据损坏。</p>
     *
     * <p>校验不过就**整批拒绝**、不做部分删除：调用方一次点删多个时，
     * 部分成功部分失败会让用户搞不清发生了什么。</p>
     */
    @CacheEvict(allEntries = true)
    @Override
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        for (Long id : ids) {
            long referenced = countReferencedGoods(id);
            if (referenced > 0) {
                throw new BusinessException(400,
                        "品牌下有 " + referenced + " 个商品，不能删除。请先删除或改挂这些商品。");
            }
        }
        return brandMapper.deleteByIds(ids);
    }

    /**
     * 统计引用了该品牌的在册商品数。
     *
     * <p>任何异常（含 Feign 降级返回失败）都当作「校验不了」抛出 503，
     * <b>而不是放行删除</b> —— fail-closed。</p>
     */
    private long countReferencedGoods(Long brandId) {
        TypedJsonResp<Long> resp;
        try {
            resp = goodFeignService.countGoodsByBrand(brandId, Boolean.FALSE);
        } catch (Exception e) {
            log.error("删除品牌前校验商品引用失败：brandId={}", brandId, e);
            throw new BusinessException(503, "无法校验商品引用（商品服务不可用），已取消删除");
        }
        if (resp == null || !resp.isSuccess() || resp.getData() == null) {
            throw new BusinessException(503, "无法校验商品引用（商品服务不可用），已取消删除");
        }
        return resp.getData();
    }
}
