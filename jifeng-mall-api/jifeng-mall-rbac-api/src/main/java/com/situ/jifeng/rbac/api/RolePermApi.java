package com.situ.jifeng.rbac.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.RolePermEntity;
import com.situ.jifeng.spi.model.search.RolePermSearchBean;
import com.situ.jifeng.spi.service.RolePermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/role-perms", produces = MediaType.APPLICATION_JSON_VALUE)
public class RolePermApi {
    private RolePermService rolePermService;

    @Autowired
    public void setRolePermService(RolePermService rolePermService) {
        this.rolePermService = rolePermService;
    }

    /**
     * 查询所有角色权限关联。limit值为-1时，表示查询全部数据
     *
     * @return 所有角色权限关联实体
     */
    @GetMapping
    public JsonResp findAll(RolePermSearchBean rpe, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<RolePermEntity> rolePerms = rolePermService.findAll(rpe, pi);
        PageInfo<?> pageInfo = new PageInfo<>(rolePerms);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        RolePermEntity rpe = rolePermService.findById(id);
        return JsonResp.success(rpe);
    }

    //查询指定角色的所有权限关联
    @GetMapping("/role/{roleId}")
    public JsonResp findByRoleId(@PathVariable Long roleId) {
        List<RolePermEntity> rpes = rolePermService.findByRoleId(roleId);
        return JsonResp.success(rpes);
    }

    /**
     * 保存角色权限关联实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody RolePermEntity rpe) {
        boolean success = rolePermService.save(rpe);
        if (success) {
            return JsonResp.success(rpe);
        } else {
            return JsonResp.fail(500, "保存角色权限关联失败");
        }
    }

    /**
     * 修改角色权限关联
     *
     * @param rpe 角色权限关联实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody RolePermEntity rpe) {
        boolean success = rolePermService.update(rpe);
        if (success) {
            return JsonResp.success(rpe);
        } else {
            return JsonResp.fail(500, "修改角色权限关联失败");
        }
    }

    /**
     * 批量删除角色权限关联
     *
     * @param ids 角色权限关联主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = rolePermService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
