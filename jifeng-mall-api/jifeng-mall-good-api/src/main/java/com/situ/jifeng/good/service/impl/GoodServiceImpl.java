package com.situ.jifeng.good.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.GoodEntity;
import com.situ.jifeng.spi.model.search.GoodSearchBean;
import com.situ.jifeng.spi.service.GoodService;
import com.situ.jifeng.good.mapper.GoodMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoodServiceImpl implements GoodService {
    private GoodMapper goodMapper;

    @Autowired
    public void setGoodMapper(GoodMapper goodMapper) {
        this.goodMapper = goodMapper;
    }

    @Override
    public List<GoodEntity> findAll(GoodSearchBean ge, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return goodMapper.findAll(ge);
        }
    }

    @Override
    public GoodEntity findById(Long id) {
        return goodMapper.findById(id);
    }

    @Override
    public boolean save(GoodEntity goodEntity) {
        // good 表的这几个标记列都是 NOT NULL，而 insert 语句把每一列都显式列了出来，
        // 所以数据库那层的 DEFAULT 0 根本不会生效——不赋值就是
        // "Column 'is_del' cannot be null"。逻辑删除标记本该由系统置初值，不该让调用方传。
        if (goodEntity.getIsDel() == null) {
            goodEntity.setIsDel(false);
        }
        if (goodEntity.getIsTakeDown() == null) {
            goodEntity.setIsTakeDown(false);
        }
        if (goodEntity.getIsHot() == null) {
            goodEntity.setIsHot(false);
        }
        if (goodEntity.getIsSeckill() == null) {
            goodEntity.setIsSeckill(false);
        }
        return goodMapper.save(goodEntity) > 0;
    }

    @Override
    public boolean update(GoodEntity goodEntity) {
        return goodMapper.update(goodEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return goodMapper.deleteByIds(ids);
    }

    @Override
    public boolean deductStock(Long id, Integer qty) {
        return goodMapper.deductStock(id, qty) > 0;
    }

    @Override
    public boolean addBackStock(Long id, Integer qty) {
        return goodMapper.addBackStock(id, qty) > 0;
    }
}
