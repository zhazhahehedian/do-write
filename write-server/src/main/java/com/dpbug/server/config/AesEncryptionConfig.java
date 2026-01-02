package com.dpbug.server.config;

import com.dpbug.common.utils.AesUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * AES 加密配置
 * 从配置文件读取加密密钥并初始化 AesUtil
 *
 * <p>配置示例：</p>
 * <pre>
 * ai:
 *   encryption:
 *     key: your-32-character-secret-key!!
 * </pre>
 *
 * @author dpbug
 * @since 2025-12-27
 */
@Slf4j
@Configuration
public class AesEncryptionConfig {

    @Value("${ai.encryption.key:D0-Writ3-32b!t}")
    private String encryptionKey;

    @PostConstruct
    public void init() {
        AesUtil.setDefaultSecretKey(encryptionKey);
        log.info("✅ AES加密密钥已初始化 (长度: {} 字符)", encryptionKey.length());

        // 安全提示
        if ("D0-Writ3-32b!t".equals(encryptionKey)) {
            log.warn("⚠️  使用默认加密密钥，生产环境请通过 ai.encryption.key 配置自定义密钥");
        }
    }
}