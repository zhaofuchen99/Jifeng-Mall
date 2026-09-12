package com.situ.jifeng.user.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.LoginParam;
import com.situ.jifeng.spi.model.LoginUserInfo;
import com.situ.jifeng.spi.model.UserEntity;
import com.situ.jifeng.spi.model.search.UserSearchBean;
import com.situ.jifeng.spi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserApi {
    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * 查询所有用户。limit值为-1时，表示查询全部数据
     *
     * @return 所有用户实体
     */
    @GetMapping
    public JsonResp findAll(UserSearchBean ue, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<UserEntity> users = userService.findAll(ue, pi);
        PageInfo<?> pageInfo = new PageInfo<>(users);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        UserEntity ue = userService.findById(id);
        return JsonResp.success(ue);
    }


    @GetMapping("/username/{username}")
    public JsonResp findByUsername(@PathVariable String username) {
        UserEntity ue = userService.findByUsername(username);
        return JsonResp.success(ue);
    }

    /**
     * 后台用户登录（公开接口），签发 admin 令牌
     */
    @PostMapping("/login")
    public JsonResp login(@RequestBody LoginParam param) {
        LoginUserInfo info = userService.login(param);
        return JsonResp.success(info);
    }

    /**
     * 保存用户实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody UserEntity ue) {
        boolean success = userService.save(ue);
        if (success) {
            return JsonResp.success(ue);
        } else {
            return JsonResp.fail(500, "保存用户失败");
        }
    }

    /**
     * 修改用户
     *
     * @param ue 用户实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody UserEntity ue) {
        boolean success = userService.update(ue);
        if (success) {
            return JsonResp.success(ue);
        } else {
            return JsonResp.fail(500, "修改用户失败");
        }
    }

    /**
     * 批量删除用户
     *
     * @param ids 用户主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = userService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}