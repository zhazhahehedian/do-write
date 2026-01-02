package com.dpbug.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置
 *
 * <p>使用 BCryptPasswordEncoder 进行密码加密，具有以下特性：
 * <ul>
 *   <li>自动加盐：每次加密都会生成随机盐值</li>
 *   <li>不可逆：无法从密文还原明文</li>
 *   <li>慢哈希：计算成本高，防止暴力破解</li>
 *   <li>盐值包含在密文中：验证时自动提取</li>
 * </ul>
 *
 * @author dpbug
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 配置密码编码器
     *
     * <p>BCrypt 强度参数说明：
     * <ul>
     *   <li>默认强度：10（推荐，平衡安全性和性能）</li>
     *   <li>强度范围：4-31（值越大越安全，但计算时间越长）</li>
     *   <li>强度每增加1，计算时间翻倍</li>
     * </ul>
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 使用默认强度 10
        // 如果需要更高安全性，可以设置为 12-14：new BCryptPasswordEncoder(12)
        return new BCryptPasswordEncoder();
    }
}
