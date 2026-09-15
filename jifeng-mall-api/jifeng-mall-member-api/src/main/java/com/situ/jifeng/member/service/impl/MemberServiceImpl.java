package com.situ.jifeng.member.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.BusinessException;
import com.situ.jifeng.common.JwtUtil;
import com.situ.jifeng.common.LoginFailureGuard;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.common.PasswordUtil;
import com.situ.jifeng.spi.model.LoginParam;
import com.situ.jifeng.spi.model.LoginUserInfo;
import com.situ.jifeng.spi.model.MemberEntity;
import com.situ.jifeng.spi.model.search.MemberSearchBean;
import com.situ.jifeng.spi.service.MemberService;
import com.situ.jifeng.member.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class MemberServiceImpl implements MemberService {

    /** BCrypt 密文格式：$2a$ / $2b$ / $2y$ + 两位 cost + 53 个 base64 字符 */
    private static final Pattern BCRYPT_PATTERN =
            Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private MemberMapper memberMapper;
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    public void setMemberMapper(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public List<MemberEntity> findAll(MemberSearchBean me, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return memberMapper.findAll(me);
        }
    }

    @Override
    public MemberEntity findById(Long id) {
        return memberMapper.findById(id);
    }

    @Override
    public MemberEntity findByAccount(String account) {
        return memberMapper.findByAccount(account);
    }

    @Override
    public boolean save(MemberEntity memberEntity) {
        encodePassword(memberEntity);
        return memberMapper.save(memberEntity) > 0;
    }

    /**
     * 修改会员。既承载后台的「编辑会员」，也承载后台的「重置密码」与会员自己的「修改密码」——
     * 传了 {@code password} 就是改密码，没传就不动密码列（update 是条件更新）。
     * 需求 FR-207 把「重置密码」放在 {@code PUT /api/members} 上，故不另开接口。
     */
    @Override
    public boolean update(MemberEntity memberEntity) {
        boolean passwordChanged = memberEntity.getPassword() != null && !memberEntity.getPassword().isBlank();
        validatePasswordStrength(memberEntity.getPassword());
        encodePassword(memberEntity);
        boolean ok = memberMapper.update(memberEntity) > 0;
        if (ok && passwordChanged) {
            // 改完密码顺手解掉登录失败锁定。这不是顺手而为：后台重置密码的典型场景，
            // 会员恰恰就是被防爆破锁住、登不进来才来找管理员的；不解锁的话
            // 管理员重置完密码，会员还得再等满 15 分钟才能进。
            LoginFailureGuard.clear(stringRedisTemplate, JwtUtil.AUDIENCE_MEMBER, accountOf(memberEntity));
        }
        return ok;
    }

    /** 取账号名：请求里通常只带 id（重置密码场景），这时回查一次库 */
    private String accountOf(MemberEntity memberEntity) {
        if (memberEntity.getAccount() != null && !memberEntity.getAccount().isBlank()) {
            return memberEntity.getAccount();
        }
        MemberEntity db = memberMapper.findById(memberEntity.getId());
        return db == null ? null : db.getAccount();
    }

    @Override
    public boolean changePassword(Long memberId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessException(400, "新密码不能为空");
        }
        validatePasswordStrength(newPassword);
        MemberEntity db = memberMapper.findById(memberId);
        if (db == null) {
            throw new BusinessException(404, "会员不存在");
        }
        if (!PasswordUtil.matches(oldPassword, db.getPassword())) {
            throw new BusinessException(400, "当前密码不正确");
        }
        MemberEntity toUpdate = new MemberEntity();
        toUpdate.setId(memberId);
        toUpdate.setPassword(newPassword);
        // 复用 update：它会做 BCrypt 加密，并顺手解除该会员的登录失败锁定
        return update(toUpdate);
    }

    /**
     * 把实体里的明文密码 BCrypt 加密后回填。
     *
     * <p>登录走 {@link PasswordUtil#matches}（BCrypt）校验。原先只有 {@link #register} 做了加密，
     * {@code save/update} 是<b>直接透传 mapper 的</b>，于是：</p>
     * <ul>
     *   <li>前台「修改密码」走 {@code PUT /api/members} → 明文入库 → 该会员再也登录不了；</li>
     *   <li>后台「会员管理 - 重置密码」同理。</li>
     * </ul>
     * <p>这与 user-api 后勤用户建号不加密是同一类问题（那边叫 bug 4）。</p>
     *
     * <p>已经是 BCrypt 密文时原样保留，避免二次加密——{@code update} 场景下前端可能
     * 原样回传从查询接口拿到的旧密文。</p>
     */
    /**
     * 密码强度下限（6-32 位）。前台注册页有同样的规则，但那只在浏览器里，
     * 绕过前端直接发请求就没了。改密码/重置密码是安全敏感动作，不能只靠前端，
     * 所以这里在服务端再拦一道——会员自助改密码和后台重置密码都走这条。
     *
     * <p>已经是 BCrypt 密文时跳过：密文长度 60 位，本来也不会被下限拦住，
     * 但显式跳过更清楚（{@code update} 允许原样回传已加密的旧值）。</p>
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.isBlank() || BCRYPT_PATTERN.matcher(password).matches()) {
            return;
        }
        if (password.length() < 6 || password.length() > 32) {
            throw new BusinessException(400, "密码长度 6-32 位");
        }
    }

    private void encodePassword(MemberEntity memberEntity) {
        String pwd = memberEntity.getPassword();
        if (pwd != null && !pwd.isBlank() && !BCRYPT_PATTERN.matcher(pwd).matches()) {
            memberEntity.setPassword(PasswordUtil.encode(pwd));
        }
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return memberMapper.deleteByIds(ids);
    }

    @Override
    public LoginUserInfo register(MemberEntity member) {
        String account = member.getAccount();
        if (account == null || account.isBlank()) {
            throw new IllegalArgumentException("账号不能为空");
        }
        // 账号唯一校验
        if (memberMapper.findByAccount(account) != null) {
            throw new IllegalArgumentException("账号已存在");
        }
        String plain = member.getPassword();
        if (plain == null || plain.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        // 密码不可逆加密
        member.setPassword(PasswordUtil.encode(plain));
        member.setEnabled(true);
        memberMapper.save(member);
        // 注册成功自动登录，返回会员令牌
        return buildLoginInfo(member);
    }

    @Override
    public LoginUserInfo login(LoginParam param) {
        String account = param.getAccount();
        if (account == null || account.isBlank()) {
            throw new IllegalArgumentException("账号不能为空");
        }
        // 防爆破第一道：处于锁定窗口内直接拒绝，连密码都不用校验（NFR-002 登录失败限流）
        LoginFailureGuard.assertNotLocked(stringRedisTemplate, JwtUtil.AUDIENCE_MEMBER, account);

        MemberEntity member = memberMapper.findByAccount(account);
        if (member == null) {
            // 账号不存在也计数：只对存在的账号计数的话，试几次看有没有被锁就能枚举出哪些账号真实存在
            LoginFailureGuard.recordFailure(stringRedisTemplate, JwtUtil.AUDIENCE_MEMBER, account);
            throw new IllegalArgumentException("账号或密码错误");
        }
        if (!Boolean.TRUE.equals(member.getEnabled())) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        if (!PasswordUtil.matches(param.getPassword(), member.getPassword())) {
            LoginFailureGuard.recordFailure(stringRedisTemplate, JwtUtil.AUDIENCE_MEMBER, account);
            throw new IllegalArgumentException("账号或密码错误");
        }
        // 登录成功，清掉此前累计的失败次数
        LoginFailureGuard.clear(stringRedisTemplate, JwtUtil.AUDIENCE_MEMBER, account);
        return buildLoginInfo(member);
    }

    private LoginUserInfo buildLoginInfo(MemberEntity member) {
        String token = JwtUtil.createToken(member.getId(), member.getAccount(), JwtUtil.AUDIENCE_MEMBER);
        return LoginUserInfo.builder()
                .token(token)
                .userId(member.getId())
                .account(member.getAccount())
                .name(member.getName())
                .audience(JwtUtil.AUDIENCE_MEMBER)
                .build();
    }
}