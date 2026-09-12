package com.situ.jifeng.rbac.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacGroupEntity;
import com.situ.jifeng.spi.model.search.RbacGroupSearchBean;
import com.situ.jifeng.spi.service.RbacGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/groups", produces = MediaType.APPLICATION_JSON_VALUE)
public class RbacGroupApi {
    private RbacGroupService rbacGroupService;

    @Autowired
    public void setRbacGroupService(RbacGroupService rbacGroupService) {
        this.rbacGroupService = rbacGroupService;
    }

    /**
     * 查询所有组。limit值为-1时，表示查询全部数据
     *
     * @return 所有组实体
     */
    @GetMapping
    public JsonResp findAll(RbacGroupSearchBean ge, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<RbacGroupEntity> groups = rbacGroupService.findAll(ge, pi);
        PageInfo<?> pageInfo = new PageInfo<>(groups);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        RbacGroupEntity ge = rbacGroupService.findById(id);
        return JsonResp.success(ge);
    }

    /**
     * 保存组实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody RbacGroupEntity ge) {
        boolean success = rbacGroupService.save(ge);
        if (success) {
            return JsonResp.success(ge);
        } else {
            return JsonResp.fail(500, "保存组失败");
        }
    }

    /**
     * 修改组
     *
     * @param ge 组实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody RbacGroupEntity ge) {
        boolean success = rbacGroupService.update(ge);
        if (success) {
            return JsonResp.success(ge);
        } else {
            return JsonResp.fail(500, "修改组失败");
        }
    }

    /**
     * 批量删除组
     *
     * @param ids 组主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = rbacGroupService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
