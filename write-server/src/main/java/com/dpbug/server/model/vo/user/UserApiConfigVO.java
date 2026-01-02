package com.dpbug.server.model.vo.user;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户API配置VO（返回给前端）
 *
 * @author dpbug
 */
@Data
public class UserApiConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * API类型：OPENAI, AZURE_OPENAI, CUSTOM
     */
    private String apiType;

    /**
     * API Key（脱敏显示，如：sk-***abc）
     */
    private String apiKeyMasked;

    /**
     * API Base URL
     */
    private String baseUrl;

    /**
     * 默认模型名称
     */
    private String modelName;

    /**
     * 最大Token数
     */
    private Integer maxTokens;

    /**
     * 温度参数
     */
    private BigDecimal temperature;

    /**
     * 是否默认配置：0-否，1-是
     */
    private Integer isDefault;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 对API Key进行脱敏处理
     *
     * @param apiKey 原始API Key
     * @return 脱敏后的API Key
     */
    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        // 显示前4个字符和后4个字符
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }
}
