package com.situ.jifeng.rbac.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacResourceEntity;
import com.situ.jifeng.spi.model.search.RbacResourceSearchBean;
import com.situ.jifeng.spi.service.RbacResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/resources", produces = MediaType.APPLICATION_JSON_VALUE)
public class RbacResourceApi {
    private RbacResourceService rbacResourceService;

    @Autowired
    public void setRbacResourceService(RbacResourceService rbacResourceService) {
        this.rbacResourceService = rbacResourceService;
    }

    /**
     * 查询所有资源。limit值为-1时，表示查询全部数据
     *
     * @return 所有资源实体
     */
    @GetMapping
    public JsonResp findAll(RbacResourceSearchBean rse, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<RbacResourceEntity> resources = rbacResourceService.findAll(rse, pi);
        PageInfo<?> pageInfo = new PageInfo<>(resources);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        RbacResourceEntity rse = rbacResourceService.findById(id);
        return JsonResp.success(rse);
    }

    /**
     * 保存资源实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody RbacResourceEntity rse) {
        boolean success = rbacResourceService.save(rse);
        if (success) {
            return JsonResp.success(rse);
        } else {
            return JsonResp.fail(500, "保存资源失败");
        }
    }

    /**
     * 修改资源
     *
     * @param rse 资源实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody RbacResourceEntity rse) {
        boolean success = rbacResourceService.update(rse);
        if (success) {
            return JsonResp.success(rse);
        } else {
            return JsonResp.fail(500, "修改资源失败");
        }
    }

    /**
     * 批量删除资源
     *
     * @param ids 资源主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = rbacResourceService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
