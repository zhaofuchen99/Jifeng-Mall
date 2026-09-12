package com.situ.jifeng.seckill.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.seckill.mapper.SeckillGoodMapper;
import com.situ.jifeng.spi.model.SeckillGoodEntity;
import com.situ.jifeng.spi.model.search.SeckillGoodSearchBean;
import com.situ.jifeng.spi.service.SeckillGoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeckillGoodServiceImpl implements SeckillGoodService {
    private SeckillGoodMapper seckillGoodMapper;

    @Autowired
    public void setSeckillGoodMapper(SeckillGoodMapper seckillGoodMapper) {
        this.seckillGoodMapper = seckillGoodMapper;
    }

    @Override
    public List<SeckillGoodEntity> findAll(SeckillGoodSearchBean sgs, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return seckillGoodMapper.findAll(sgs);
        }
    }

    @Override
    public SeckillGoodEntity findById(Long id) {
        return seckillGoodMapper.findById(id);
    }

    @Override
    public boolean save(SeckillGoodEntity seckillGoodEntity) {
        // seckill_good 的 stock / sold / limit_per_user 都是 NOT NULL，而 insert 语句
        // 把每一列都显式列出来了，数据库的 DEFAULT 0 不会生效。
        // sold 是系统累加的已售数，新增时必须是 0，不该由调用方传。
        if (seckillGoodEntity.getSold() == null) {
            seckillGoodEntity.setSold(0);
        }
        if (seckillGoodEntity.getStock() == null) {
            seckillGoodEntity.setStock(0);
        }
        if (seckillGoodEntity.getLimitPerUser() == null) {
            seckillGoodEntity.setLimitPerUser(1);
        }
        return seckillGoodMapper.save(seckillGoodEntity) > 0;
    }

    @Override
    public boolean update(SeckillGoodEntity seckillGoodEntity) {
        return seckillGoodMapper.update(seckillGoodEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return seckillGoodMapper.deleteByIds(ids);
    }

    @Override
    public List<SeckillGoodEntity> findBySeckillId(Long seckillId) {
        return seckillGoodMapper.findBySeckillId(seckillId);
    }
}
