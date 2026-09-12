package com.situ.jifeng.spi.service;


import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.CategoryEntity;
import com.situ.jifeng.spi.model.search.CategorySearchBean;

import java.util.List;

public interface CategoryService {
    List<CategoryEntity> findAll(CategorySearchBean ce, PaginateInfo pi);

    CategoryEntity findById(Long id);

    boolean save(CategoryEntity categoryEntity);

    boolean update(CategoryEntity categoryEntity);

    int deleteByIds(List<Long> ids);

    List<CategoryEntity> findAll();

    List<CategoryEntity> findTree();
}
