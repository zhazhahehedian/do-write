package com.dpbug.common.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256 加密解密工具类
 * <p>
 * 使用 AES/CBC/PKCS5Padding 模式
 * 密钥长度：256位
 *
 * @author dpbug
 */
@Slf4j
public class AesUtil {

    /**
     * 算法名称
     */
    private static final String ALGORITHM = "AES";

    /**
     * 加密模式
     */
    private static final String CIPHER_MODE = "AES/CBC/PKCS5Padding";

    /**
     * 密钥长度（256位）
     */
    private static final int KEY_SIZE = 256;

    /**
     * IV向量长度（128位 = 16字节）
     */
    private static final int IV_SIZE = 16;

    /**
     * 默认密钥（可通过 setDefaultSecretKey 方法动态设置）
     * 生产环境应通过配置文件注入不同的密钥
     */
    private static String DEFAULT_SECRET_KEY = "D0-Writ3-32b!t";

    /**
     * 设置默认密钥（从配置文件注入）
     *
     * @param secretKey 密钥字符串
     */
    public static void setDefaultSecretKey(String secretKey) {
        if (secretKey != null && !secretKey.isEmpty()) {
            DEFAULT_SECRET_KEY = secretKey;
        }
    }

    /**
     * 加密
     *
     * @param plainText 明文
     * @return 密文（Base64编码，格式：IV + 密文）
     */
    public static String encrypt(String plainText) {
        return encrypt(plainText, DEFAULT_SECRET_KEY);
    }

    /**
     * 加密
     *
     * @param plainText 明文
     * @param secretKey 密钥
     * @return 密文（Base64编码，格式：IV + 密文）
     */
    public static String encrypt(String plainText, String secretKey) {
        try {
            // 生成密钥
            SecretKeySpec keySpec = generateKey(secretKey);

            // 生成随机IV
            byte[] iv = generateIV();
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 创建加密器
            Cipher cipher = Cipher.getInstance(CIPHER_MODE);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            // 加密
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 将IV和密文拼接
            byte[] combined = new byte[IV_SIZE + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_SIZE);
            System.arraycopy(encrypted, 0, combined, IV_SIZE, encrypted.length);

            // Base64编码
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES加密失败", e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密
     *
     * @param cipherText 密文（Base64编码）
     * @return 明文
     */
    public static String decrypt(String cipherText) {
        return decrypt(cipherText, DEFAULT_SECRET_KEY);
    }

    /**
     * 解密
     *
     * @param cipherText 密文（Base64编码）
     * @param secretKey  密钥
     * @return 明文
     */
    public static String decrypt(String cipherText, String secretKey) {
        try {
            // Base64解码
            byte[] combined = Base64.getDecoder().decode(cipherText);

            // 提取IV和密文
            byte[] iv = new byte[IV_SIZE];
            byte[] encrypted = new byte[combined.length - IV_SIZE];
            System.arraycopy(combined, 0, iv, 0, IV_SIZE);
            System.arraycopy(combined, IV_SIZE, encrypted, 0, encrypted.length);

            // 生成密钥
            SecretKeySpec keySpec = generateKey(secretKey);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 创建解密器
            Cipher cipher = Cipher.getInstance(CIPHER_MODE);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            // 解密
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES解密失败", e);
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 生成密钥
     *
     * @param secretKey 密钥字符串
     * @return SecretKeySpec
     */
    private static SecretKeySpec generateKey(String secretKey) {
        try {
            // 使用SHA-256对密钥进行哈希，确保密钥长度为32字节（256位）
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

            // 如果密钥长度不足32字节，使用KeyGenerator生成
            if (keyBytes.length < 32) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
                SecureRandom secureRandom = new SecureRandom(keyBytes);
                keyGenerator.init(KEY_SIZE, secureRandom);
                SecretKey key = keyGenerator.generateKey();
                return new SecretKeySpec(key.getEncoded(), ALGORITHM);
            }

            // 取前32字节作为密钥
            byte[] key = new byte[32];
            System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));
            return new SecretKeySpec(key, ALGORITHM);
        } catch (Exception e) {
            log.error("生成密钥失败", e);
            throw new RuntimeException("生成密钥失败", e);
        }
    }

    /**
     * 生成随机IV
     *
     * @return IV字节数组
     */
    private static byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    /**
     * 生成随机密钥（256位）
     *
     * @return Base64编码的密钥
     */
    public static String generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            log.error("生成密钥失败", e);
            throw new RuntimeException("生成密钥失败", e);
        }
    }
}