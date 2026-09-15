package com.situ.jifeng.category.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.BusinessException;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.common.TypedJsonResp;
import com.situ.jifeng.spi.model.CategoryEntity;
import com.situ.jifeng.spi.model.search.CategorySearchBean;
import com.situ.jifeng.spi.service.CategoryService;
import com.situ.jifeng.category.mapper.CategoryMapper;
import com.situ.jifeng.category.service.GoodFeignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private CategoryMapper categoryMapper;
    private GoodFeignService goodFeignService;

    @Autowired
    public void setCategoryMapper(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Autowired
    public void setGoodFeignService(GoodFeignService goodFeignService) {
        this.goodFeignService = goodFeignService;
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

    /**
     * 级联删除分类树节点。<b>删除前逐个校验商品引用</b>（设计文档 5.3「删除校验子分类与商品引用」）。
     *
     * <p>校验必须覆盖<b>整棵子树</b>：本方法是级联删除，删父分类会连子分类一起删，
     * 所以只要子树里任何一个节点挂着商品，整批都不该删 —— 否则那个商品会变成
     * 指向不存在分类的孤儿（{@code GET /api/goods?full=true} 组装分类时得到 null）。</p>
     *
     * <p>校验不过就**整批拒绝**，不做部分删除。</p>
     */
    //级联删除树节点
    @CacheEvict(allEntries = true)
    @Override
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<CategoryEntity> tree = self().findTree();//查询树结构，用以查询后代结点

        // 1) 先把待删的整棵子树收集齐，逐节点校验商品引用
        List<CategoryEntity> subtree = new ArrayList<>();
        for (Long id : ids) {
            CategoryEntity ce = _findById(tree, id);
            if (ce != null) {
                _collect(ce, subtree);
            }
        }
        for (CategoryEntity ce : subtree) {
            long referenced = countReferencedGoods(ce.getId());
            if (referenced > 0) {
                throw new BusinessException(400,
                        "分类「" + ce.getName() + "」下有 " + referenced + " 个商品，不能删除。"
                                + "注意删除父分类会级联删除子分类，子分类下的商品同样会被孤立。");
            }
        }

        // 2) 校验通过后才真正删除（逻辑与原先一致，只是前移了校验）
        int total = 0;
        for (Long id : ids) {
            //查询指定id对应的节点，返回的节点拥有树结构
            CategoryEntity ce = _findById(tree, id);
            if (ce != null) {
                total += _delete(ce);
            }
        }
        return total;
    }

    /** 深度优先收集「自身 + 全部后代」，用于校验级联删除会波及到的每一个节点 */
    private void _collect(CategoryEntity node, List<CategoryEntity> acc) {
        acc.add(node);
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (CategoryEntity child : node.getChildren()) {
                _collect(child, acc);
            }
        }
    }

    /**
     * 统计引用了该分类的在册商品数。
     *
     * <p>任何异常（含 Feign 降级返回失败）都当作「校验不了」抛 503，
     * <b>而不是放行删除</b> —— fail-closed。</p>
     */
    private long countReferencedGoods(Long categoryId) {
        TypedJsonResp<Long> resp;
        try {
            resp = goodFeignService.countGoodsByCategory(categoryId, Boolean.FALSE);
        } catch (Exception e) {
            log.error("删除分类前校验商品引用失败：categoryId={}", categoryId, e);
            throw new BusinessException(503, "无法校验商品引用（商品服务不可用），已取消删除");
        }
        if (resp == null || !resp.isSuccess() || resp.getData() == null) {
            throw new BusinessException(503, "无法校验商品引用（商品服务不可用），已取消删除");
        }
        return resp.getData();
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
