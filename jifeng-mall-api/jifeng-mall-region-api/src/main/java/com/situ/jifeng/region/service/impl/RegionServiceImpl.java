package com.situ.jifeng.region.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.BusinessException;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RegionEntity;
import com.situ.jifeng.spi.model.search.RegionSearchBean;
import com.situ.jifeng.spi.service.RegionService;
import com.situ.jifeng.region.mapper.RegionMapper;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 行政区划（FR-210）。
 *
 * <p>树形结构的三个不变量，全部由本类维护，调用方传入什么都会被改写：</p>
 * <ol>
 *   <li><b>level 由 parent 推导</b>，不接受客户端传值：根为 1，其余为父级 +1，
 *       最深 3 级（省/市/区）。</li>
 *   <li><b>parentId 为 null 或 0 都归一化成 0</b>（DDL 里 {@code parent_id NOT NULL DEFAULT 0}，
 *       0 表示根）。</li>
 *   <li><b>改父级要连带修正所有后代的 level</b>，否则层级列会留下脏数据，
 *       而按 level 查询的路径（如 {@code findByParentId(0)} 取省级）依赖它。</li>
 * </ol>
 *
 * <p><b>缓存</b>：本服务的 findByParentId / findById 走 @Cacheable（Redis，TTL 30 天）。
 * 写操作一律 {@code @CacheEvict(allEntries = true)} 清空整个 cacheNames ——
 * 区划数据只有几十行，全清比精确驱逐更省心，也避免漏掉
 * {@code findByParentId-0} 这种"改了 A 但脏了 B"的间接依赖。
 * 不清缓存的话，前台省市区三级联动和地址回显会一直读到改动前的数据。</p>
 */
@CacheConfig(cacheNames = "c.s.s.region.service.impl.RegionServiceImpl")
@Service
public class RegionServiceImpl implements RegionService {

    /** 行政区划最深三级：省 / 市 / 区 */
    private static final int MAX_LEVEL = 3;

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

    @CacheEvict(allEntries = true)
    @Override
    public boolean save(RegionEntity region) {
        if (region.getName() == null || region.getName().trim().isEmpty()) {
            throw new BusinessException(400, "区划名称不能为空");
        }
        region.setName(region.getName().trim());

        // 显式指定的 id 就是行政区划编码（如 440000），重复了要拦住，
        // 否则会以 SQLIntegrityConstraintViolationException 的形态冒出来
        if (region.getId() != null && regionMapper.findById(region.getId()) != null) {
            throw new BusinessException(400, "区划编码 " + region.getId() + " 已存在");
        }

        region.setParentId(normalizeParentId(region.getParentId()));
        region.setLevel(deriveLevel(region.getParentId()));
        region.setSortOrder(region.getSortOrder() == null ? 0 : region.getSortOrder());

        return regionMapper.save(region) > 0;
    }

    @CacheEvict(allEntries = true)
    @Override
    public boolean update(RegionEntity region) {
        if (region.getId() == null) {
            throw new BusinessException(400, "缺少区划编号");
        }
        RegionEntity old = regionMapper.findById(region.getId());
        if (old == null) {
            throw new BusinessException(404, "区划不存在：" + region.getId());
        }
        if (region.getName() != null) {
            if (region.getName().trim().isEmpty()) {
                throw new BusinessException(400, "区划名称不能为空");
            }
            region.setName(region.getName().trim());
        }

        // 不传 parentId 表示"不动父级"，不能当成"移到根"——否则一次只改名称的
        // 局部更新会把节点悄悄提到顶层，它的 level 也跟着变
        Long parentId = region.getParentId() == null
                ? normalizeParentId(old.getParentId())
                : normalizeParentId(region.getParentId());
        if (!parentId.equals(old.getParentId())) {
            // 改父级：不能挂到自己或自己的后代下面，否则树会成环、递归操作全部死循环
            if (parentId.equals(region.getId()) || isDescendant(region.getId(), parentId)) {
                throw new BusinessException(400, "不能把区划挂到它自己或它的下级下面");
            }
        }
        region.setParentId(parentId);

        int oldLevel = old.getLevel() == null ? 1 : old.getLevel();
        int newLevel = deriveLevel(parentId);
        region.setLevel(newLevel);

        boolean success = regionMapper.update(region) > 0;

        // 节点整体上下移动了，其后代也得跟着挪，否则 level 列自相矛盾
        if (success && newLevel != oldLevel) {
            shiftDescendantLevels(region.getId(), newLevel);
        }
        return success;
    }

    /**
     * 级联删除。与分类管理一致：删「广东省」会连同「广州市/天河区」一起删掉。
     *
     * <p>先整棵子树收集齐、再一条 {@code delete ... in (...)} 发出去，而不是逐层递归删 ——
     * 后者每层一次往返，且中途失败会留下"子没了父还在"的残树。</p>
     */
    @CacheEvict(allEntries = true)
    @Override
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<Long> all = new ArrayList<>();
        for (Long id : ids) {
            if (id == null || all.contains(id)) {
                continue;
            }
            all.add(id);
            collectDescendants(id, all);
        }
        return regionMapper.deleteByIds(all);
    }

    /** 深度优先收集后代编号（自身不含） */
    private void collectDescendants(Long id, List<Long> acc) {
        for (RegionEntity child : regionMapper.findByParentId(id)) {
            if (acc.contains(child.getId())) {
                continue; // 理论上不会发生，防脏数据导致环
            }
            acc.add(child.getId());
            collectDescendants(child.getId(), acc);
        }
    }

    /** 后代整体改层级：新层级 = 父级新层级 +1，逐层往下推 */
    private void shiftDescendantLevels(Long parentId, int parentLevel) {
        for (RegionEntity child : regionMapper.findByParentId(parentId)) {
            RegionEntity patch = new RegionEntity();
            patch.setId(child.getId());
            patch.setLevel(parentLevel + 1);
            regionMapper.update(patch);
            shiftDescendantLevels(child.getId(), parentLevel + 1);
        }
    }

    /** targetId 是不是 id 的后代（用于改父级时的成环检测） */
    private boolean isDescendant(Long id, Long targetId) {
        if (targetId == null || targetId <= 0) {
            return false;
        }
        for (RegionEntity child : regionMapper.findByParentId(id)) {
            if (child.getId().equals(targetId) || isDescendant(child.getId(), targetId)) {
                return true;
            }
        }
        return false;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId <= 0 ? 0L : parentId;
    }

    /** 由父级推导层级：根为 1，其余为父级 +1；父级不存在或超深都报错 */
    private int deriveLevel(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return 1;
        }
        RegionEntity parent = regionMapper.findById(parentId);
        if (parent == null) {
            throw new BusinessException(400, "上级区划不存在：" + parentId);
        }
        int level = (parent.getLevel() == null ? 1 : parent.getLevel()) + 1;
        if (level > MAX_LEVEL) {
            throw new BusinessException(400, "行政区划最多三级（省/市/区），不能再往下加");
        }
        return level;
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
