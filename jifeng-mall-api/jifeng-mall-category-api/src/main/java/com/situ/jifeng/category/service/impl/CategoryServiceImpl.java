package com.situ.jifeng.category.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.CategoryEntity;
import com.situ.jifeng.spi.model.search.CategorySearchBean;
import com.situ.jifeng.spi.service.CategoryService;
import com.situ.jifeng.category.mapper.CategoryMapper;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CacheConfig(cacheNames = "c.s.s.category.service.impl.CategoryServiceImpl")
@Service
public class CategoryServiceImpl implements CategoryService {
    private CategoryMapper categoryMapper;

    @Autowired
    public void setCategoryMapper(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryEntity> findAll(CategorySearchBean ce, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return categoryMapper.findAll(ce);
        }
    }

    //非树型结构
    @Cacheable(key = "'findById-'+#id")
    @Override
    public CategoryEntity findById(Long id) {
        return categoryMapper.findById(id);
    }

    @CacheEvict(allEntries = true)
    @Override
    public boolean save(CategoryEntity categoryEntity) {
        return categoryMapper.save(categoryEntity) > 0;
    }

    @CacheEvict(allEntries = true)
    @Override
    public boolean update(CategoryEntity categoryEntity) {
        return categoryMapper.update(categoryEntity) > 0;
    }

    //级联删除树节点
    @CacheEvict(allEntries = true)
    @Override
    public int deleteByIds(List<Long> ids) {
        int total = 0;
        List<CategoryEntity> tree = self().findTree();//查询树结构，用以查询后代结点
        for (Long id : ids) {
            //查询指定id对应的节点，返回的节点拥有树结构
            CategoryEntity ce = _findById(tree, id);
            if (ce != null) {
                total += _delete(ce);
            }
        }
        return total;
    }

    //代理对象
    private CategoryService self() {
        return (CategoryService) AopContext.currentProxy();
    }

    @Cacheable(key = "'findAll'")
    @Override
    public List<CategoryEntity> findAll() {
        CategorySearchBean ce = new CategorySearchBean();
        PaginateInfo pi = PaginateInfo.from(1, 0);
        return findAll(ce, pi);
    }

    @Cacheable(key = "'findTree'")
    @Override
    public List<CategoryEntity> findTree() {
        List<CategoryEntity> categories = self().findAll();
        return buildTree(categories);
    }

    //构建树，返回根节点数组
    private List<CategoryEntity> buildTree(List<CategoryEntity> all) {
        //缓存所有节点
        Map<Long, CategoryEntity> cache = all.stream()
                .collect(Collectors.toMap(CategoryEntity::getId, t -> t));

        //所有根节点
        List<CategoryEntity> roots = new ArrayList<>();

        for (CategoryEntity t : cache.values()) {
            Long parentId = t.getParentId();//父节点编号
            // 根节点的判断必须同时认 null 和 0：
            // DDL 里 `parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父分类（0 为根）'`，
            // 种子数据也全是 0。原先只判 null，一级分类会走进下面的 else 分支，
            // cache.get(0L) 取到 null → 抛"无效的父节点编号:0" → 被兜底处理器吞成 500。
            // 也就是说 GET /api/categories/tree 在这个表结构下从来没成功过
            // （首页分类导航、商品列表左侧分类、面包屑全依赖它）。
            if (parentId == null || parentId == 0L) {
                roots.add(t);
            } else {
                CategoryEntity parent = cache.get(parentId);
                if (parent == null) {
                    throw new RuntimeException("无效的父节点编号:" + parentId);
                }
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(t);
                t.setParent(parent);
            }
        }
        return roots;
    }

    /**
     * 深度优先，递归搜索，返回树型结构节点
     *
     * @param all 所有待查询节点
     * @param id  目标节点编号
     * @return 目标节点
     */
    private CategoryEntity _findById(List<CategoryEntity> all, Long id) {
        for (CategoryEntity ce : all) {
            if (ce.getId().equals(id)) {
                return ce;
            }

            if (ce.getChildren() != null && !ce.getChildren().isEmpty()) {
                CategoryEntity exist = _findById(ce.getChildren(), id);
                if (exist != null) {
                    return exist;
                }
            }
        }
        return null;
    }

    //级联删除分类
    private int _delete(CategoryEntity category) {
        int total = 0;
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            for (CategoryEntity child : category.getChildren()) {
                total += _delete(child);
            }
        }
        //物理删除自身
        total += _deleteById(category.getId());
        return total;
    }

    //批量物理删除。此方法无法被外界调用，也无法被AOP，同时此方法仅有一条sql，无需事务
    private int _deleteById(Long id) {
        return categoryMapper.deleteByIds(List.of(id));
    }
}
