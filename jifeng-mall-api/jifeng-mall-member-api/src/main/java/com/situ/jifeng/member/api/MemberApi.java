package com.situ.jifeng.member.api;

import com.github.pagehelper.PageInfo;
import com.situ.jifeng.common.BusinessException;
import com.situ.jifeng.common.JsonResp;
import com.situ.jifeng.common.JwtUtil;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.spi.model.ChangePasswordDTO;
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
    public JsonResp findById(@PathVariable Long id,
                             @RequestHeader(name = "X-User-Id", required = false) String userId,
                             @RequestHeader(name = "X-Audience", required = false) String audience) {
        assertSelf(id, userId, null, null, audience);
        MemberEntity me = memberService.findById(id);
        return JsonResp.success(me);
    }

    @GetMapping("/account/{account}")
    public JsonResp findByAccount(@PathVariable String account,
                                  @RequestHeader(name = "X-User-Name", required = false) String user,
                                  @RequestHeader(name = "X-Audience", required = false) String audience) {
        assertSelf(null, null, account, user, audience);
        MemberEntity me = memberService.findByAccount(account);
        return JsonResp.success(me);
    }

    /**
     * 会员只能查看自己的资料（需求 7.2-3 的同类要求）。
     *
     * <p>判定规则与 order-api 保持一致：</p>
     * <ul>
     *   <li>无 {@code X-Audience} 头 → 走的是服务间直连而非网关，放行。
     *       （本服务的查询接口目前没有服务间调用方，这条规则是为了与 order-api 统一，
     *       那里 seckill-api 确实会用 Feign 调 {@code /api/orders/seckill-no/{sno}}）</li>
     *   <li>{@code admin} → 后台会员管理要看任意会员，放行</li>
     *   <li>{@code member} → 按 id 或 account 比对令牌里的身份</li>
     * </ul>
     *
     * <p>注意比对 id 时必须先确认 audience 是 member：{@code member} 表与 {@code user} 表的
     * 主键是两套独立空间，后台账号的 userId 和某个会员的 id 完全可能相同。</p>
     */
    private void assertSelf(Long id, String userId, String account, String user, String audience) {
        if (audience == null || JwtUtil.AUDIENCE_ADMIN.equals(audience)) {
            return;
        }
        boolean idOk = id == null || (userId != null && userId.equals(String.valueOf(id)));
        boolean accountOk = account == null || (user != null && user.equals(account));
        if (!idOk || !accountOk) {
            throw new BusinessException(403, "无权查看他人资料");
        }
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
    public JsonResp update(@RequestBody MemberEntity me,
                           @RequestHeader(name = "X-Audience", required = false) String audience) {
        // 这是**后台**的会员管理接口（可改任意会员，传了 password 就是重置密码）。
        // 会员自助改资料/改密码走下面那两个 /id/{id} 开头的接口。
        // 网关已按路径把会员挡在外面，这里再兜一道，避免只依赖网关一层。
        assertAdmin(audience, "修改会员");
        boolean success = memberService.update(me);
        if (success) {
            return JsonResp.success(me);
        } else {
            return JsonResp.fail(500, "修改会员失败");
        }
    }

    /**
     * 会员自助修改资料（前台「个人信息」页）。
     *
     * <p><b>为什么不能用上面的裸 {@code PUT /api/members}</b>：那是后台的会员管理接口，
     * 网关的 {@code MEMBER_PATHS} 里只有 {@code /api/members/id/**} 与
     * {@code /api/members/account/**}，裸路径把会员挡住了。而如果为了放行它把裸路径加进白名单，
     * 由于本方法此前没有归属校验，<b>任何会员都能改别人的密码完成账号接管</b>。
     * 所以会员的自助入口一律走已经放行且自带归属校验的 {@code /api/members/id/{id}} 前缀。</p>
     *
     * <p>请求体里的 {@code password} 会被<b>强制丢弃</b>，改密码只能走下面那个接口——
     * 否则就绕过了那里的旧密码校验。</p>
     */
    @PutMapping("/id/{id}")
    public JsonResp updateSelf(@PathVariable Long id, @RequestBody MemberEntity me,
                               @RequestHeader(name = "X-User-Id", required = false) String userId,
                               @RequestHeader(name = "X-Audience", required = false) String audience) {
        assertSelf(id, userId, null, null, audience);
        me.setId(id);
        me.setPassword(null);
        me.setAccount(null);
        me.setEnabled(null);
        boolean success = memberService.update(me);
        if (success) {
            return JsonResp.success(me);
        } else {
            return JsonResp.fail(500, "修改会员失败");
        }
    }

    /**
     * 会员自助修改密码（前台「修改密码」页）。
     *
     * <p>旧密码由<b>服务端</b>校验。原先前台是「拿旧密码再走一次登录接口」间接验证的，
     * 那层校验只在浏览器里，绕过前端直接发请求就能跳过——令牌被盗时会把真正的会员锁在门外。</p>
     */
    @PutMapping("/id/{id}/password")
    public JsonResp changePassword(@PathVariable Long id, @RequestBody ChangePasswordDTO dto,
                                   @RequestHeader(name = "X-User-Id", required = false) String userId,
                                   @RequestHeader(name = "X-Audience", required = false) String audience) {
        assertSelf(id, userId, null, null, audience);
        boolean success = memberService.changePassword(id, dto.getOldPassword(), dto.getNewPassword());
        if (success) {
            return JsonResp.success(true);
        } else {
            return JsonResp.fail(500, "修改密码失败");
        }
    }

    /**
     * 后台专属动作：会员即使拿着合法令牌也不该能调后台的会员管理接口。
     * 与 {@link #assertSelf} 一致，放行"无 X-Audience 头"的服务间直连。
     */
    private void assertAdmin(String audience, String action) {
        if (audience != null && !JwtUtil.AUDIENCE_ADMIN.equals(audience)) {
            throw new BusinessException(403, "无权限执行" + action + "操作");
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