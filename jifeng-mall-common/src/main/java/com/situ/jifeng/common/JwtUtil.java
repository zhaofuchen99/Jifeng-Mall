package com.situ.jifeng.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类。
 *
 * <p>依据详细设计说明书 6.1 / 8.2：
 * <ul>
 *   <li>HS256 签名（生产建议切换 RSA 公私钥，见设计文档；此处按设计采用 HS256）</li>
 *   <li>token 中携带 userId、username、audience（admin / member）</li>
 *   <li>有效期：admin 12 小时、member 7 天（可通过环境变量 JWT_SECRET 自定义密钥）</li>
 * </ul>
 * </p>
 */
public final class JwtUtil {

    /** 令牌类型：后台用户 */
    public static final String AUDIENCE_ADMIN = "admin";
    /** 令牌类型：会员 */
    public static final String AUDIENCE_MEMBER = "member";

    /** 密钥（HS256 至少 32 字节）。生产建议改为配置中心下发并对密钥做 RSA 化。 */
    private static final String SECRET =
            System.getenv("JWT_SECRET") != null
                    ? System.getenv("JWT_SECRET")
                    : "jifeng-mall-secret-key-please-change-in-production-256bit!!";

    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /** admin 有效期：12 小时（毫秒） */
    private static final long ADMIN_EXPIRE_MILLIS = 12L * 60L * 60L * 1000L;
    /** member 有效期：7 天（毫秒） */
    private static final long MEMBER_EXPIRE_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    private JwtUtil() {
    }

    /**
     * 生成令牌。
     *
     * @param userId   用户/会员主键
     * @param username 登录账号（admin 为 username，member 为 account）
     * @param audience admin / member
     * @return JWT 字符串
     */
    public static String createToken(Long userId, String username, String audience) {
        long expireMillis = AUDIENCE_ADMIN.equals(audience)
                ? ADMIN_EXPIRE_MILLIS
                : MEMBER_EXPIRE_MILLIS;
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("audience", audience)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireMillis))
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析令牌。
     *
     * @param token JWT 字符串
     * @return Claims
     * @throws JwtException 令牌非法、过期或签名不符时抛出
     */
    public static Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验令牌是否有效（签名正确且未过期）。
     */
    public static boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
