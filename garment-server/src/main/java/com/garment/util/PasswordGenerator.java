package com.garment.util;

import java.security.SecureRandom;

/**
 * 随机密码生成器
 * <p>
 * 字符集仅含字母+数字，并排除易混字符（O/0/I/1/l），方便人工抄录。
 * 使用 {@link SecureRandom} 保证密码学强度的随机性。
 */
public final class PasswordGenerator {

    /** 排除了 O / I / L / 0 / 1 / l 等易混字符的字符集 */
    private static final char[] CHARS =
            "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {}

    /**
     * 生成 8 位随机密码（用于老板/员工临时密码）
     */
    public static String generate() {
        return generate(8);
    }

    /**
     * 生成指定位数的随机密码
     *
     * @param length 密码长度，必须大于 0
     */
    public static String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("密码长度必须大于 0");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS[RANDOM.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }
}
