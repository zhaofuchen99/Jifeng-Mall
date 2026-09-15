package com.situ.jifeng.region.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RegionEntity;
import com.situ.jifeng.spi.model.search.RegionSearchBean;
import com.situ.jifeng.spi.service.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行政区划接口。
 *
 * <p><b>鉴权分层</b>：GET 全部公开（前台收货地址的省市区三级联动要免登录可用），
 * 写操作（POST/PUT/DELETE）由网关的权限判定拦住 —— 网关对 {@code /api/regions/**}
 * 只放行 GET，写请求会走 RBAC，要求资源 {@code /api/regions/**}（种子里是 2021）。</p>
 */
@RestController
@RequestMapping(value = "/api/regions", produces = MediaType.APPLICATION_JSON_VALUE)
public class RegionApi {
    private RegionService regionService;

    @Autowired
    public void setRegionService(RegionService regionService) {
        this.regionService = regionService;
    }

    /**
     * 查询所有地区。limit值为-1时，表示查询全部数据
     *
     * @return 所有地区实体
     */
    @GetMapping
    public JsonResp findAll(RegionSearchBean re, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<RegionEntity> provinces = regionService.findAll(re, pi);
        PageInfo<?> pageInfo = new PageInfo<>(provinces);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        RegionEntity re = regionService.findById(id);
        return JsonResp.success(re);
    }

    @GetMapping("/parent-id/{id}")
    public JsonResp findByParentId(@PathVariable Long id) {
        List<RegionEntity> res = regionService.findByParentId(id);
        return JsonResp.success(res);
    }

    /**
     * 新增区划（后台「地区管理」）。
     *
     * <p>level 不接受请求体传值，由 service 按 parentId 推导；id 可填行政区划编码，
     * 留空则自增。</p>
     */
    @PostMapping
    public JsonResp save(@RequestBody RegionEntity region) {
        boolean success = regionService.save(region);
        if (success) {
            return JsonResp.success(region);
        } else {
            return JsonResp.fail(500, "保存区划失败");
        }
    }

    @PutMapping
    public JsonResp update(@RequestBody RegionEntity region) {
        boolean success = regionService.update(region);
        if (success) {
            return JsonResp.success(region);
        } else {
            return JsonResp.fail(500, "修改区划失败");
        }
    }

    /**
     * 批量删除区划。<b>会连同下级一起删除</b>（树形维护语义，与分类管理一致）。
     *
     * @return data 为实际删除的行数，含被级联删除的下级
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = regionService.deleteByIds(List.of(ids));
        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}