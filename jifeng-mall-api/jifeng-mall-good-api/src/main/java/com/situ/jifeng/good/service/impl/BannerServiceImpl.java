package com.situ.jifeng.good.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.BusinessException;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.good.mapper.BannerMapper;
import com.situ.jifeng.spi.model.BannerEntity;
import com.situ.jifeng.spi.model.search.BannerSearchBean;
import com.situ.jifeng.spi.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BannerServiceImpl implements BannerService {
    private BannerMapper bannerMapper;

    @Autowired
    public void setBannerMapper(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    @Override
    public List<BannerEntity> findAll(BannerSearchBean bsb, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return bannerMapper.findAll(bsb);
        }
    }

    @Override
    public BannerEntity findById(Long id) {
        return bannerMapper.findById(id);
    }

    @Override
    public boolean save(BannerEntity bannerEntity) {
        // title / sort_no / enabled 都是 NOT NULL，而 insert 语句把每一列都显式列了出来，
        // 数据库那层的 DEFAULT 不会生效——sort_no 不赋值就是 "Column 'sort_no' cannot be null"。
        // 排序号与启用标记属于系统初值，不该要求调用方一个个传。
        // （这个 NOT NULL 模式在本项目里已经出现过 7 次，见 README/备忘。）
        if (bannerEntity.getSortNo() == null) {
            bannerEntity.setSortNo(0);
        }
        if (bannerEntity.getEnabled() == null) {
            bannerEntity.setEnabled(true);
        }
        // title 是调用方必须给的，数据库报错太晦涩（"Column 'title' cannot be null"），
        // 这里先给一句人话
        if (bannerEntity.getTitle() == null || bannerEntity.getTitle().isBlank()) {
            throw new BusinessException(400, "轮播标题不能为空");
        }
        return bannerMapper.save(bannerEntity) > 0;
    }

    @Override
    public boolean update(BannerEntity bannerEntity) {
        return bannerMapper.update(bannerEntity) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return bannerMapper.deleteByIds(ids);
    }
}
