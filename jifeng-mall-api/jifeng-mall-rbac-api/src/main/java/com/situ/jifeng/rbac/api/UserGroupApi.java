package com.situ.jifeng.rbac.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.UserGroupEntity;
import com.situ.jifeng.spi.model.search.UserGroupSearchBean;
import com.situ.jifeng.spi.service.UserGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/user-groups", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserGroupApi {
    private UserGroupService userGroupService;

    @Autowired
    public void setUserGroupService(UserGroupService userGroupService) {
        this.userGroupService = userGroupService;
    }

    /**
     * 查询所有用户组关联。limit值为-1时，表示查询全部数据
     *
     * @return 所有用户组关联实体
     */
    @GetMapping
    public JsonResp findAll(UserGroupSearchBean uge, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<UserGroupEntity> userGroups = userGroupService.findAll(uge, pi);
        PageInfo<?> pageInfo = new PageInfo<>(userGroups);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        UserGroupEntity uge = userGroupService.findById(id);
        return JsonResp.success(uge);
    }

    //查询指定组的所有用户关联
    @GetMapping("/group/{groupId}")
    public JsonResp findByGroupId(@PathVariable Long groupId) {
        List<UserGroupEntity> uges = userGroupService.findByGroupId(groupId);
        return JsonResp.success(uges);
    }

    //查询指定用户的所有组关联
    @GetMapping("/user/{userId}")
    public JsonResp findByUserId(@PathVariable Long userId) {
        List<UserGroupEntity> uges = userGroupService.findByUserId(userId);
        return JsonResp.success(uges);
    }

    /**
     * 保存用户组关联实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody UserGroupEntity uge) {
        boolean success = userGroupService.save(uge);
        if (success) {
            return JsonResp.success(uge);
        } else {
            return JsonResp.fail(500, "保存用户组关联失败");
        }
    }

    /**
     * 修改用户组关联
     *
     * @param uge 用户组关联实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody UserGroupEntity uge) {
        boolean success = userGroupService.update(uge);
        if (success) {
            return JsonResp.success(uge);
        } else {
            return JsonResp.fail(500, "修改用户组关联失败");
        }
    }

    /**
     * 批量删除用户组关联
     *
     * @param ids 用户组关联主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = userGroupService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}
