package com.dpbug.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * AesUtil 测试类
 *
 * @author dpbug
 */
class AesUtilTest {

    @Test
    void testEncryptDecrypt() {
        String plainText = "sk-1234567890abcdefghijklmnopqrstuvwxyz";

        // 加密
        String encrypted = AesUtil.encrypt(plainText);
        System.out.println("加密后: " + encrypted);

        // 解密
        String decrypted = AesUtil.decrypt(encrypted);
        System.out.println("解密后: " + decrypted);

        // 验证
        assertEquals(plainText, decrypted);
    }

    @Test
    void testSetDefaultSecretKey() {
        String customKey = "my-custom-32-character-key!!!";
        AesUtil.setDefaultSecretKey(customKey);

        String plainText = "test-api-key";
        String encrypted = AesUtil.encrypt(plainText);
        String decrypted = AesUtil.decrypt(encrypted);

        assertEquals(plainText, decrypted);
    }

    @Test
    void testEncryptWithCustomKey() {
        String plainText = "secret-message";
        String customKey = "another-32-character-secret!!";

        // 使用自定义密钥加密
        String encrypted = AesUtil.encrypt(plainText, customKey);

        // 使用相同密钥解密
        String decrypted = AesUtil.decrypt(encrypted, customKey);

        assertEquals(plainText, decrypted);
    }

    @Test
    void testGenerateSecretKey() {
        String key1 = AesUtil.generateSecretKey();
        String key2 = AesUtil.generateSecretKey();

        System.out.println("生成的密钥1: " + key1);
        System.out.println("生成的密钥2: " + key2);

        // 每次生成的密钥应该不同
        assertNotEquals(key1, key2);
    }
}