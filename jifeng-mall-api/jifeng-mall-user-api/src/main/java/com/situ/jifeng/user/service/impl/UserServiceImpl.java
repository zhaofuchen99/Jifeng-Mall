package com.situ.jifeng.user.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.situ.jifeng.common.JwtUtil;
import com.situ.jifeng.common.LoginFailureGuard;
import com.situ.jifeng.common.PaginateInfo;
import com.situ.jifeng.common.PasswordUtil;
import com.situ.jifeng.spi.model.LoginParam;
import com.situ.jifeng.spi.model.LoginUserInfo;
import com.situ.jifeng.spi.model.UserEntity;
import com.situ.jifeng.spi.model.search.UserSearchBean;
import com.situ.jifeng.spi.service.UserService;
import com.situ.jifeng.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {

    /** BCrypt 密文格式：$2a$ / $2b$ / $2y$ + 两位 cost + 53 个 base64 字符 */
    private static final Pattern BCRYPT_PATTERN =
            Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private UserMapper userMapper;
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public List<UserEntity> findAll(UserSearchBean ue, PaginateInfo pi) {
        try (Page<?> _ = PageHelper.startPage(pi.pageNo(), pi.pageSize())) {
            return userMapper.findAll(ue);
        }
    }

    @Override
    public UserEntity findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public UserEntity findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public boolean save(UserEntity userEntity) {
        encodePassword(userEntity);
        // user 表的 enabled / status / locked / login_times 都是 NOT NULL，
        // 而 insert 语句把每一列都显式列出来了，数据库的 DEFAULT 不会生效，
        // 不设值就是 "Column 'login_times' cannot be null"。
        // 这些都是账号的初始状态，不该要求调用方一个个传。
        if (userEntity.getEnabled() == null) {
            userEntity.setEnabled(true);
        }
        if (userEntity.getStatus() == null) {
            userEntity.setStatus(1);
        }
        if (userEntity.getLocked() == null) {
            userEntity.setLocked(false);
        }
        if (userEntity.getLoginTimes() == null) {
            userEntity.setLoginTimes(0);
        }
        return userMapper.save(userEntity) > 0;
    }

    @Override
    public boolean update(UserEntity userEntity) {
        encodePassword(userEntity);
        return userMapper.update(userEntity) > 0;
    }

    /**
     * 把实体里的明文密码 BCrypt 加密后回填。
     *
     * <p>登录走 {@link PasswordUtil#matches}（BCrypt）校验。原先 {@code save/update} 直接透传 mapper，
     * 导致通过 {@code POST /api/users} 建出来的后台用户<b>密码以明文入库、且永远登录失败</b>
     * （实测：建 testuser/test123 → 库里存 "test123" → 登录返回"用户名或密码错误"）。
     * 会员注册 {@code MemberServiceImpl} 一直是加密的，只有后台建用户这条路径漏了。</p>
     *
     * <p>已经是 BCrypt 密文时原样保留，避免二次加密——{@code update} 场景下前端可能
     * 原样回传已加密的旧值。</p>
     */
    private void encodePassword(UserEntity userEntity) {
        String pwd = userEntity.getPassword();
        if (pwd != null && !pwd.isBlank() && !BCRYPT_PATTERN.matcher(pwd).matches()) {
            userEntity.setPassword(PasswordUtil.encode(pwd));
        }
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        return userMapper.deleteByIds(ids);
    }

    @Override
    public LoginUserInfo login(LoginParam param) {
        String username = param.getAccount();
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        // 防爆破第一道：处于锁定窗口内直接拒绝，连密码都不用校验（需求 6.2 / NFR-002）
        LoginFailureGuard.assertNotLocked(stringRedisTemplate, JwtUtil.AUDIENCE_ADMIN, username);

        UserEntity user = userMapper.findByUsername(username);
        if (user == null) {
            // 账号不存在也计数：只对存在的账号计数的话，试几次看有没有被锁就能枚举出哪些账号真实存在
            LoginFailureGuard.recordFailure(stringRedisTemplate, JwtUtil.AUDIENCE_ADMIN, username);
            throw new IllegalArgumentException("用户名或密码错误");
        }
        // 账号状态校验（启用 / 锁定 / 过期）。
        // 这几种失败不计入防爆破——账号本来就被封着，猜密码也进不去，
        // 计数只会让管理员更难判断到底是哪一种。
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        if (Boolean.TRUE.equals(user.getLocked())) {
            throw new IllegalArgumentException("账号已被锁定（管理员手工锁定，需后台解锁）");
        }
        if (user.getUserExpireTime() != null && user.getUserExpireTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("账号已过期");
        }
        if (user.getCredentialExpireTime() != null && user.getCredentialExpireTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("凭证已过期，请修改密码");
        }
        if (!PasswordUtil.matches(param.getPassword(), user.getPassword())) {
            LoginFailureGuard.recordFailure(stringRedisTemplate, JwtUtil.AUDIENCE_ADMIN, username);
            throw new IllegalArgumentException("用户名或密码错误");
        }
        // 登录成功，清掉此前累计的失败次数
        LoginFailureGuard.clear(stringRedisTemplate, JwtUtil.AUDIENCE_ADMIN, username);

        // 更新登录信息
        user.setLoginTimes(user.getLoginTimes() == null ? 1 : user.getLoginTimes() + 1);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.update(user);

        String token = JwtUtil.createToken(user.getId(), user.getUsername(), JwtUtil.AUDIENCE_ADMIN);
        return LoginUserInfo.builder()
                .token(token)
                .userId(user.getId())
                .account(user.getUsername())
                .name(user.getUsername())
                .audience(JwtUtil.AUDIENCE_ADMIN)
                .build();
    }
}