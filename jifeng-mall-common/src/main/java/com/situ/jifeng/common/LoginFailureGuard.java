package com.situ.jifeng.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 登录失败次数限制与锁定（需求 6.2「登录防爆破：错误次数限制与锁定」、NFR-002「登录失败限流」）。
 *
 * <p>实现放在 Redis 而不是数据库：</p>
 * <ul>
 *   <li>失败计数是<b>短期</b>状态，用 TTL 让锁定<b>自动到期</b>，不需要定时任务去解锁；</li>
 *   <li>不必给 {@code member} 表加字段——该表没有 {@code locked} 列，而 {@code user.locked}
 *       已经被「后台手工锁定账号」占用，两者语义不同，混用会出现
 *       「防爆破自动锁的账号管理员解不开 / 管理员锁的账号 15 分钟后自动开」的混乱。</li>
 * </ul>
 *
 * <p><b>与 {@code user.locked} 的分工</b>：{@code user.locked} 是管理员的手工硬锁，
 * 不会自动解除；本类的锁定是临时的，窗口过了自动放行。两者都会拦登录，提示语不同。</p>
 *
 * <p><b>为什么失败时不区分「账号不存在」</b>：账号不存在同样计数。若只对存在的账号计数，
 * 攻击者试 5 次后看有没有被锁，就能区分出哪些账号真实存在（账号枚举）。
 * 代价是所有人都知道账号名就能把别人锁 15 分钟——这是账号维度锁定固有的取舍，
 * 真实系统靠 IP 维度限流 + 验证码缓解。</p>
 *
 * <p><b>Redis 不可用时放行（fail-open）</b>：与网关 RBAC 判定失败时的 fail-closed 刻意相反。
 * 授权失败必须拒绝，否则等于把权限校验关掉；而防爆破是纵深防御的一层，
 * Redis 挂了不该把<b>整个登录</b>也带走——那等于用可用性换一个次要的安全加固。
 * 放行会打 WARN 日志，便于发现。</p>
 */
public final class LoginFailureGuard {

    private static final Logger log = LoggerFactory.getLogger(LoginFailureGuard.class);

    /** 同一账号连续失败多少次后锁定 */
    private static final int MAX_FAILURES = 5;

    /** 锁定时长；同时也是失败计数的滑动窗口（每次失败刷新，窗口内累计到阈值就锁） */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final String KEY_PREFIX = "login:fail:";

    private LoginFailureGuard() {
    }

    /**
     * 尝试登录前调用：若处于锁定窗口内，直接抛 {@link BusinessException}(429)，
     * 连密码都不用校验。
     */
    public static void assertNotLocked(StringRedisTemplate redis, String audience, String account) {
        if (account == null || account.isBlank()) {
            return;
        }
        try {
            String value = redis.opsForValue().get(key(audience, account));
            int failures = parseInt(value);
            if (failures >= MAX_FAILURES) {
                long minutes = remainingMinutes(redis, audience, account);
                throw new BusinessException(429,
                        "登录失败次数过多，账号已锁定，请 " + minutes + " 分钟后重试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("登录失败计数读取失败，本次不做锁定判断：audience={}, account={}", audience, account, e);
        }
    }

    /**
     * 密码校验失败后调用：失败次数 +1 并刷新窗口。达到阈值时抛 {@link BusinessException}(429)，
     * 调用方不必自己判断——直接让异常冒出去即可。
     */
    public static void recordFailure(StringRedisTemplate redis, String audience, String account) {
        if (account == null || account.isBlank()) {
            return;
        }
        try {
            String k = key(audience, account);
            Long failures = redis.opsForValue().increment(k);
            // 每次失败都刷新 TTL：锁定从「最后一次失败」开始算，而不是从第一次
            redis.expire(k, LOCK_DURATION);
            if (failures != null && failures >= MAX_FAILURES) {
                log.warn("账号因登录失败次数过多被锁定：audience={}, account={}, failures={}", audience, account, failures);
                throw new BusinessException(429,
                        "登录失败次数过多，账号已锁定，请 " + LOCK_DURATION.toMinutes() + " 分钟后重试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("登录失败计数写入失败，本次不计入：audience={}, account={}", audience, account, e);
        }
    }

    /** 登录成功后调用：清掉失败计数，此前累计的失败不再影响后续登录 */
    public static void clear(StringRedisTemplate redis, String audience, String account) {
        if (account == null || account.isBlank()) {
            return;
        }
        try {
            redis.delete(key(audience, account));
        } catch (Exception e) {
            log.warn("登录失败计数清除失败：audience={}, account={}", audience, account, e);
        }
    }

    /** 失败次数（0 表示没有失败记录），仅用于排查与测试 */
    public static int failures(StringRedisTemplate redis, String audience, String account) {
        try {
            return parseInt(redis.opsForValue().get(key(audience, account)));
        } catch (Exception e) {
            return 0;
        }
    }

    /** 锁定剩余时间（分钟，向上取整，至少 1）；没有锁定记录时返回 0 */
    public static long remainingMinutes(StringRedisTemplate redis, String audience, String account) {
        try {
            Long seconds = redis.getExpire(key(audience, account), TimeUnit.SECONDS);
            if (seconds == null || seconds <= 0) {
                return 0;
            }
            return Math.max(1, (seconds + 59) / 60);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String key(String audience, String account) {
        return KEY_PREFIX + audience + ":" + account;
    }

    private static int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
