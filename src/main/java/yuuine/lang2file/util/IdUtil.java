package yuuine.lang2file.util;

import java.security.SecureRandom;

/**
 * ID生成工具类，提供生成高随机性、全局唯一会话ID的方法。
 * 使用密码学安全的伪随机数生成器 {@link SecureRandom} 保证随机质量，
 * 生成的ID由大小写字母和数字组成，长度为32位，适合作为会话标识。
 *
 * <p>该类为工具类，不可实例化，所有方法均为静态方法。
 *
 * @author your-name
 * @version 1.0
 */
public final class IdUtil {

    // 私有构造器，防止实例化
    private IdUtil() {
    }

    // 字符集：26个大写字母 + 26个小写字母 + 10个数字，共62个字符
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final char[] CHAR_ARRAY = CHARS.toCharArray();
    private static final int CHARS_LENGTH = CHAR_ARRAY.length;

    // 密码学安全的随机数生成器（线程安全）
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // 默认生成的会话ID长度
    private static final int SESSION_ID_LENGTH = 32;

    /**
     * 生成一个32位的会话ID。
     * <p>
     * 会话ID由大小写字母和数字组成，使用 {@link SecureRandom} 确保随机性和不可预测性。
     * 长度为32位，理论上碰撞概率极低，适合作为全局唯一的会话标识。
     *
     * @return 一个32位的随机字符串
     */
    public static String generateSessionId() {
        StringBuilder sb = new StringBuilder(SESSION_ID_LENGTH);
        for (int i = 0; i < SESSION_ID_LENGTH; i++) {
            // 从字符集中随机选取一个字符
            int index = SECURE_RANDOM.nextInt(CHARS_LENGTH);
            sb.append(CHAR_ARRAY[index]);
        }
        return sb.toString();
    }
}