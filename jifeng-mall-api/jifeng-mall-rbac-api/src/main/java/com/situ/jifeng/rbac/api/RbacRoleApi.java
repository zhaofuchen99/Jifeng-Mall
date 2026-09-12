package com.situ.jifeng.rbac.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RbacRoleEntity;
import com.situ.jifeng.spi.model.search.RbacRoleSearchBean;
import com.situ.jifeng.spi.service.RbacRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/roles", produces = MediaType.APPLICATION_JSON_VALUE)
public class RbacRoleApi {
    private RbacRoleService rbacRoleService;

    @Autowired
    public void setRbacRoleService(RbacRoleService rbacRoleService) {
        this.rbacRoleService = rbacRoleService;
    }

    /**
     * 查询所有角色。limit值为-1时，表示查询全部数据
     *
     * @return 所有角色实体
     */
    @GetMapping
    public JsonResp findAll(RbacRoleSearchBean re, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<RbacRoleEntity> roles = rbacRoleService.findAll(re, pi);
        PageInfo<?> pageInfo = new PageInfo<>(roles);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        RbacRoleEntity re = rbacRoleService.findById(id);
        return JsonResp.success(re);
    }

    /**
     * 保存角色实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody RbacRoleEntity re) {
        boolean success = rbacRoleService.save(re);
        if (success) {
            return JsonResp.success(re);
        } else {
            return JsonResp.fail(500, "保存角色失败");
        }
    }

    /**
     * 修改角色
     *
     * @param re 角色实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody RbacRoleEntity re) {
        boolean success = rbacRoleService.update(re);
        if (success) {
            return JsonResp.success(re);
        } else {
            return JsonResp.fail(500, "修改角色失败");
        }
    }

    /**
     * 批量删除角色
     *
     * @param ids 角色主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = rbacRoleService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
