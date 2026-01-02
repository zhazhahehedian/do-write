package com.dpbug.server.model.entity.user;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dpbug.server.model.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 用户API配置实体类
 *
 * @author dpbug
 * @since 2025-12-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_api_config")
public class UserApiConfig extends BaseEntity {

    /**
     * 用户ID (0表示系统配置)
     */
    private Long userId;

    /**
     * AI提供商类型: OPENAI, OLLAMA等
     */
    private String apiType;

    /**
     * 配置名称 (用户自定义，如"我的GPT4配置")
     */
    private String configName;

    /**
     * API密钥 (AES-256加密存储)
     * 注意: 从数据库读取后需要解密，保存前需要加密
     */
    private String apiKey;

    /**
     * API基础URL
     */
    private String baseUrl;

    /**
     * 模型名称 (如gpt-4o, deepseek-r1)
     */
    private String modelName;

    /**
     * 温度参数 (0.0-2.0)
     */
    private BigDecimal temperature;

    /**
     * 最大Token数
     */
    private Integer maxTokens;

    /**
     * 嵌入模型名称 (如text-embedding-3-large)
     */
    private String embeddingModel;

    /**
     * 是否为默认配置 (每个用户只能有一个默认配置)
     */
    private Integer isDefault;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
