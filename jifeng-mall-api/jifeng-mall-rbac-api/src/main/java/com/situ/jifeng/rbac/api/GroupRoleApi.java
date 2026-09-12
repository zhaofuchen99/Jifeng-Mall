package com.situ.jifeng.rbac.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.GroupRoleEntity;
import com.situ.jifeng.spi.model.search.GroupRoleSearchBean;
import com.situ.jifeng.spi.service.GroupRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/group-roles", produces = MediaType.APPLICATION_JSON_VALUE)
public class GroupRoleApi {
    private GroupRoleService groupRoleService;

    @Autowired
    public void setGroupRoleService(GroupRoleService groupRoleService) {
        this.groupRoleService = groupRoleService;
    }

    /**
     * 查询所有组角色关联。limit值为-1时，表示查询全部数据
     *
     * @return 所有组角色关联实体
     */
    @GetMapping
    public JsonResp findAll(GroupRoleSearchBean gre, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<GroupRoleEntity> groupRoles = groupRoleService.findAll(gre, pi);
        PageInfo<?> pageInfo = new PageInfo<>(groupRoles);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        GroupRoleEntity gre = groupRoleService.findById(id);
        return JsonResp.success(gre);
    }

    //查询指定组的所有角色关联
    @GetMapping("/group/{groupId}")
    public JsonResp findByGroupId(@PathVariable Long groupId) {
        List<GroupRoleEntity> gres = groupRoleService.findByGroupId(groupId);
        return JsonResp.success(gres);
    }

    /**
     * 保存组角色关联实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody GroupRoleEntity gre) {
        boolean success = groupRoleService.save(gre);
        if (success) {
            return JsonResp.success(gre);
        } else {
            return JsonResp.fail(500, "保存组角色关联失败");
        }
    }

    /**
     * 修改组角色关联
     *
     * @param gre 组角色关联实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody GroupRoleEntity gre) {
        boolean success = groupRoleService.update(gre);
        if (success) {
            return JsonResp.success(gre);
        } else {
            return JsonResp.fail(500, "修改组角色关联失败");
        }
    }

    /**
     * 批量删除组角色关联
     *
     * @param ids 组角色关联主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = groupRoleService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
