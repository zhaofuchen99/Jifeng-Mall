package com.situ.jifeng.member.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.LoginParam;
import com.situ.jifeng.spi.model.LoginUserInfo;
import com.situ.jifeng.spi.model.MemberEntity;
import com.situ.jifeng.spi.model.search.MemberSearchBean;
import com.situ.jifeng.spi.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/members", produces = MediaType.APPLICATION_JSON_VALUE)
public class MemberApi {
    private MemberService memberService;

    @Autowired
    public void setMemberService(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 查询所有会员。limit值为-1时，表示查询全部数据
     *
     * @return 所有会员实体
     */
    @GetMapping
    public JsonResp findAll(MemberSearchBean me, @RequestParam(defaultValue = "1") Integer pageNo,
                            @RequestParam(defaultValue = "0") Integer pageSize) {
        PaginateInfo pi = PaginateInfo.from(pageNo, pageSize);
        List<MemberEntity> members = memberService.findAll(me, pi);
        PageInfo<?> pageInfo = new PageInfo<>(members);
        return JsonResp.success(pageInfo);
    }

    @GetMapping("/id/{id}")
    public JsonResp findById(@PathVariable Long id) {
        MemberEntity me = memberService.findById(id);
        return JsonResp.success(me);
    }

    @GetMapping("/account/{account}")
    public JsonResp findByAccount(@PathVariable String account) {
        MemberEntity me = memberService.findByAccount(account);
        return JsonResp.success(me);
    }

    /**
     * 会员注册（公开接口）
     */
    @PostMapping("/register")
    public JsonResp register(@RequestBody MemberEntity me) {
        LoginUserInfo info = memberService.register(me);
        return JsonResp.success(info);
    }

    /**
     * 会员登录（公开接口），成功签发会员令牌
     */
    @PostMapping("/login")
    public JsonResp login(@RequestBody LoginParam param) {
        LoginUserInfo info = memberService.login(param);
        return JsonResp.success(info);
    }

    /**
     * 保存会员实体
     *
     * @return 响应结果
     */
    @PostMapping
    public JsonResp save(@RequestBody MemberEntity me) {
        boolean success = memberService.save(me);
        if (success) {
            return JsonResp.success(me);
        } else {
            return JsonResp.fail(500, "保存会员失败");
        }
    }

    /**
     * 修改会员
     *
     * @param me 会员实体
     * @return 响应结果
     */
    @PutMapping
    public JsonResp update(@RequestBody MemberEntity me) {
        boolean success = memberService.update(me);
        if (success) {
            return JsonResp.success(me);
        } else {
            return JsonResp.fail(500, "修改会员失败");
        }
    }

    /**
     * 批量删除会员
     *
     * @param ids 会员主键集合
     * @return 删除结果
     */
    @DeleteMapping
    public JsonResp deleteByIds(@RequestBody Long[] ids) {
        int rows = memberService.deleteByIds(List.of(ids));

        if (rows == 0) {
            return JsonResp.fail(500, "删除失败");
        } else {
            return JsonResp.success(rows);
        }
    }
}