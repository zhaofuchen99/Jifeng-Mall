package com.situ.jifeng.member.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.JwtUtil;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.common.PasswordUtil;
import com.situ.jifeng.spi.model.LoginParam;
import com.situ.jifeng.spi.model.LoginUserInfo;
import com.situ.jifeng.spi.model.MemberEntity;
import com.situ.jifeng.spi.model.search.MemberSearchBean;
import com.situ.jifeng.spi.service.MemberService;
import com.situ.jifeng.member.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class MemberServiceImpl implements MemberService {

    /** BCrypt 密文格式：$2a$ / $2b$ / $2y$ + 两位 cost + 53 个 base64 字符 */
    private static final Pattern BCRYPT_PATTERN =
            Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private MemberMapper memberMapper;

    @Autowired
    public void setMemberMapper(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
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

    @Override
    public boolean update(MemberEntity memberEntity) {
        encodePassword(memberEntity);
        return memberMapper.update(memberEntity) > 0;
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
        if (param.getAccount() == null || param.getAccount().isBlank()) {
            throw new IllegalArgumentException("账号不能为空");
        }
        MemberEntity member = memberMapper.findByAccount(param.getAccount());
        if (member == null) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        if (!Boolean.TRUE.equals(member.getEnabled())) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        if (!PasswordUtil.matches(param.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
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