package com.dpbug.server.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 用户API配置DTO
 *
 * @author dpbug
 */
@Data
public class UserApiConfigRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID（更新时需要）
     */
    private Long id;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 配置名称
     */
    @NotBlank(message = "配置名称不能为空")
    @Size(max = 100, message = "配置名称长度不能超过100")
    private String configName;

    /**
     * API类型：OPENAI, AZURE_OPENAI, CUSTOM
     */
    @NotBlank(message = "API类型不能为空")
    private String apiType;

    /**
     * API Key（明文，保存时会自动加密）
     */
    @NotBlank(message = "API Key不能为空")
    @Size(max = 500, message = "API Key长度不能超过500")
    private String apiKey;

    /**
     * API Base URL（可选，用于自定义端点）
     */
    @Size(max = 255, message = "Base URL长度不能超过255")
    private String baseUrl;

    /**
     * 默认模型名称
     */
    @Size(max = 50, message = "模型名称长度不能超过50")
    private String modelName;

    /**
     * 最大Token数
     */
    @Min(value = 1, message = "最大Token数必须大于0")
    @Max(value = 100000, message = "最大Token数不能超过100000")
    private Integer maxTokens;

    /**
     * 温度参数（0.0-2.0）
     */
    @DecimalMin(value = "0.0", message = "温度参数不能小于0.0")
    @DecimalMax(value = "2.0", message = "温度参数不能大于2.0")
    private BigDecimal temperature;

    /**
     * 是否默认配置：0-否，1-是
     */
    private Integer isDefault;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    /**
     * 配置状态
     */
    private Integer status;
}
