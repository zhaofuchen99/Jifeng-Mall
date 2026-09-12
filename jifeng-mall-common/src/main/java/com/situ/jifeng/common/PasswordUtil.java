package com.situ.jifeng.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具（BCrypt）。
 *
 * <p>依据设计文档：密码须不可逆加密存储，禁止明文。此处统一使用 Spring Security 的
 * BCryptPasswordEncoder（每次加密随机盐，校验用 matches）。</p>
 */
public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    /**
     * 明文加密。
     */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 明文与密文校验。
     *
     * @return true 匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return ENCODER.matches(rawPassword, encodedPassword);
    }
}
