package com.dpbug.server.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dpbug.server.model.dto.user.UserApiConfigRequest;
import com.dpbug.server.model.entity.user.UserApiConfig;
import com.dpbug.server.model.vo.user.UserApiConfigVO;

import java.util.List;

/**
 * 用户API配置服务接口
 *
 * @author dpbug
 * @since 2025-12-27
 */
public interface UserApiConfigService extends IService<UserApiConfig> {

    /**
     * 保存用户API配置（API Key会自动加密）
     *
     * @param dto 配置DTO
     * @return 配置ID
     */
    Long saveConfig(UserApiConfigRequest dto);

    /**
     * 更新用户API配置
     *
     * @param dto 配置DTO
     * @return 是否成功
     */
    boolean updateConfig(UserApiConfigRequest dto);

    /**
     * 删除用户API配置
     *
     * @param userId   用户ID
     * @param configId 配置ID
     * @return 是否成功
     */
    boolean deleteConfig(Long userId, Long configId);

    /**
     * 设置默认配置
     *
     * @param userId   用户ID
     * @param configId 配置ID
     * @return 是否成功
     */
    boolean setDefaultConfig(Long userId, Long configId);

    /**
     * 获取用户的默认API配置（解密后）
     *
     * @param userId 用户ID
     * @return API配置
     */
    UserApiConfig getDefaultConfig(Long userId);

    /**
     * 根据ID获取配置（解密后）
     *
     * @param userId   用户ID
     * @param configId 配置ID
     * @return API配置
     */
    UserApiConfig getConfigById(Long userId, Long configId);

    /**
     * 获取用户的所有API配置（脱敏显示）
     *
     * @param userId 用户ID
     * @return 配置列表
     */
    List<UserApiConfigVO> listUserConfigs(Long userId);

    /**
     * 验证API配置是否有效
     *
     * @param dto 配置DTO
     * @return 是否有效
     */
    boolean validateConfig(UserApiConfigRequest dto);

    /**
     * 获取系统配置 (user_id=0)
     * 用于提供全局Embedding模型
     *
     * @return 系统配置
     */
    UserApiConfig getSystemConfig();

    /**
     * 解密API密钥
     *
     * @param encryptedKey 加密的密钥
     * @return 明文密钥
     */
    String decryptApiKey(String encryptedKey);

    /**
     * 获取用户所有配置列表（未解密）
     *
     * @param userId 用户ID
     * @return 配置列表
     */
    List<UserApiConfig> listByUserId(Long userId);
}