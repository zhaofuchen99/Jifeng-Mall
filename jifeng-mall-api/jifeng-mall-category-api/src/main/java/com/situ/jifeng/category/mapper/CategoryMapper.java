package com.situ.jifeng.category.mapper;

import com.situ.jifeng.spi.model.CategoryEntity;
import com.situ.jifeng.spi.model.search.CategorySearchBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {
    List<CategoryEntity> findAll(CategorySearchBean ce);

    CategoryEntity findById(Long id);

    int save(CategoryEntity categoryEntity);

    int update(CategoryEntity categoryEntity);

    int deleteByIds(List<Long> ids);
}
