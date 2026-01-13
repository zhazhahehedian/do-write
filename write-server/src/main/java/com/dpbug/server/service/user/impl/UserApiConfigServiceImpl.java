package com.dpbug.server.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dpbug.common.enums.ResultCode;
import com.dpbug.common.exception.BusinessException;
import com.dpbug.common.utils.AesUtil;
import com.dpbug.common.utils.Assert;
import com.dpbug.server.ai.OpenAiBaseUrlNormalizer;
import com.dpbug.server.mapper.user.UserApiConfigMapper;
import com.dpbug.server.model.dto.user.UserApiConfigRequest;
import com.dpbug.server.model.entity.user.UserApiConfig;
import com.dpbug.server.model.vo.user.UserApiConfigVO;
import com.dpbug.server.service.user.UserApiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户API配置服务实现
 *
 * @author dpbug
 * @since 2025-12-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApiConfigServiceImpl extends ServiceImpl<UserApiConfigMapper, UserApiConfig>
        implements UserApiConfigService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveConfig(UserApiConfigRequest dto) {
        // 参数校验
        Assert.notNull(dto.getUserId(), "用户ID不能为空");
        Assert.notBlank(dto.getConfigName(), "配置名称不能为空");
        Assert.notBlank(dto.getApiKey(), "API Key不能为空");

        // 检查配置名称是否重复
        LambdaQueryWrapper<UserApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserApiConfig::getUserId, dto.getUserId())
                .eq(UserApiConfig::getConfigName, dto.getConfigName());
        if (this.count(wrapper) > 0) {
            throw new BusinessException("配置名称已存在");
        }

        // 创建配置实体
        UserApiConfig config = new UserApiConfig();
        BeanUtils.copyProperties(dto, config);
        config.setBaseUrl(normalizeBaseUrlForApiType(config.getApiType(), dto.getBaseUrl()));

        // 加密API Key
        config.setApiKey(AesUtil.encrypt(dto.getApiKey()));

        // 检查用户是否已有配置，如果没有则自动设为默认
        LambdaQueryWrapper<UserApiConfig> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(UserApiConfig::getUserId, dto.getUserId());
        boolean isFirstConfig = this.count(countWrapper) == 0;

        // 设置默认值
        if (config.getIsDefault() == null) {
            // 如果是用户的第一条配置，自动设为默认
            config.setIsDefault(isFirstConfig ? 1 : 0);
        }
        if (config.getStatus() == null) {
            config.setStatus(1);
        }

        // 如果设置为默认配置，取消其他配置的默认状态
        if (config.getIsDefault() == 1) {
            baseMapper.cancelDefaultByUserId(dto.getUserId());
        }

        // 保存配置
        this.save(config);
        log.info("用户{}保存API配置成功，配置ID：{}", dto.getUserId(), config.getId());

        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfig(UserApiConfigRequest dto) {
        // 参数校验
        Assert.notNull(dto.getId(), "配置ID不能为空");
        Assert.notNull(dto.getUserId(), "用户ID不能为空");

        // 查询配置
        UserApiConfig config = this.getById(dto.getId());
        Assert.notNull(config, "配置不存在");
        Assert.isTrue(config.getUserId().equals(dto.getUserId()), "无权限操作此配置");

        // 检查配置名称是否重复
        if (dto.getConfigName() != null && !dto.getConfigName().equals(config.getConfigName())) {
            LambdaQueryWrapper<UserApiConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserApiConfig::getUserId, dto.getUserId())
                    .eq(UserApiConfig::getConfigName, dto.getConfigName())
                    .ne(UserApiConfig::getId, dto.getId());
            if (this.count(wrapper) > 0) {
                throw new BusinessException("配置名称已存在");
            }
        }

        // 更新配置
        if (dto.getConfigName() != null) {
            config.setConfigName(dto.getConfigName());
        }
        if (dto.getApiType() != null) {
            config.setApiType(dto.getApiType());
        }
        if (dto.getApiKey() != null) {
            // 加密新的API Key
            config.setApiKey(AesUtil.encrypt(dto.getApiKey()));
        }
        if (dto.getBaseUrl() != null) {
            String apiTypeForNormalize = dto.getApiType() != null ? dto.getApiType() : config.getApiType();
            config.setBaseUrl(normalizeBaseUrlForApiType(apiTypeForNormalize, dto.getBaseUrl()));
        }
        if (dto.getModelName() != null) {
            config.setModelName(dto.getModelName());
        }
        if (dto.getMaxTokens() != null) {
            config.setMaxTokens(dto.getMaxTokens());
        }
        if (dto.getTemperature() != null) {
            config.setTemperature(dto.getTemperature());
        }
        if (dto.getRemark() != null) {
            config.setRemark(dto.getRemark());
        }

        // 处理状态变更
        if (dto.getStatus() != null) {
            // 如果禁用了当前默认配置，自动取消默认状态
            if (dto.getStatus() == 0 && config.getIsDefault() == 1) {
                config.setIsDefault(0);
                log.warn("用户{}禁用了默认配置，已自动取消默认状态，配置ID：{}", dto.getUserId(), config.getId());
            }
            config.setStatus(dto.getStatus());
        }

        // 处理默认配置的设置/取消
        if (dto.getIsDefault() != null) {
            if (dto.getIsDefault() == 1) {
                // 设置为默认配置
                if (config.getStatus() == 0) {
                    throw new BusinessException("已禁用的配置无法设置为默认");
                }
                baseMapper.cancelDefaultByUserId(dto.getUserId());
                config.setIsDefault(1);
                log.info("用户{}设置配置为默认，配置ID：{}", dto.getUserId(), config.getId());
            } else if (dto.getIsDefault() == 0 && config.getIsDefault() == 1) {
                // 取消默认配置
                config.setIsDefault(0);
                log.warn("用户{}取消了默认配置，配置ID：{}", dto.getUserId(), config.getId());
            }
        }

        boolean result = this.updateById(config);
        log.info("用户{}更新API配置成功，配置ID：{}", dto.getUserId(), config.getId());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfig(Long userId, Long configId) {
        // 参数校验
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(configId, "配置ID不能为空");

        // 查询配置
        UserApiConfig config = this.getById(configId);
        Assert.notNull(config, "配置不存在");
        Assert.isTrue(config.getUserId().equals(userId), "无权限操作此配置");

        // 如果删除的是默认配置，记录警告日志
        if (config.getIsDefault() == 1) {
            log.warn("用户{}删除了默认API配置，配置ID：{}，请注意重新设置默认配置", userId, configId);
        }

        // 删除配置
        boolean result = this.removeById(configId);
        log.info("用户{}删除API配置成功，配置ID：{}", userId, configId);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefaultConfig(Long userId, Long configId) {
        // 参数校验
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(configId, "配置ID不能为空");

        // 查询配置
        UserApiConfig config = this.getById(configId);
        Assert.notNull(config, "配置不存在");
        Assert.isTrue(config.getUserId().equals(userId), "无权限操作此配置");
        Assert.isTrue(config.getStatus() == 1, "配置已禁用，无法设置为默认");

        // 取消其他配置的默认状态
        baseMapper.cancelDefaultByUserId(userId);

        // 设置为默认配置
        config.setIsDefault(1);
        boolean result = this.updateById(config);
        log.info("用户{}设置默认API配置成功，配置ID：{}", userId, configId);

        return result;
    }

    @Override
    public UserApiConfig getDefaultConfig(Long userId) {
        // 参数校验
        Assert.notNull(userId, "用户ID不能为空");

        // 查询默认配置
        UserApiConfig config = baseMapper.selectDefaultByUserId(userId);
        if (config == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "用户未配置默认API");
        }

        // 解密API Key
        config.setApiKey(AesUtil.decrypt(config.getApiKey()));

        return config;
    }

    @Override
    public UserApiConfig getConfigById(Long userId, Long configId) {
        // 参数校验
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notNull(configId, "配置ID不能为空");

        // 查询配置
        UserApiConfig config = this.getById(configId);
        Assert.notNull(config, "配置不存在");
        Assert.isTrue(config.getUserId().equals(userId), "无权限访问此配置");

        // 解密API Key
        config.setApiKey(AesUtil.decrypt(config.getApiKey()));

        return config;
    }

    @Override
    public List<UserApiConfigVO> listUserConfigs(Long userId) {
        // 参数校验
        Assert.notNull(userId, "用户ID不能为空");

        // 查询用户的所有配置
        LambdaQueryWrapper<UserApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserApiConfig::getUserId, userId)
                .orderByDesc(UserApiConfig::getIsDefault)
                .orderByDesc(UserApiConfig::getCreateTime);

        List<UserApiConfig> configs = this.list(wrapper);

        // 转换为VO，脱敏显示API Key
        return configs.stream().map(config -> {
            UserApiConfigVO vo = new UserApiConfigVO();
            BeanUtils.copyProperties(config, vo);
            // 解密后脱敏显示
            String decryptedKey = AesUtil.decrypt(config.getApiKey());
            vo.setApiKeyMasked(UserApiConfigVO.maskApiKey(decryptedKey));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean validateConfig(UserApiConfigRequest dto) {
        try {
            // 基本参数校验
            Assert.notBlank(dto.getApiKey(), "API Key不能为空");
            Assert.notBlank(dto.getApiType(), "API类型不能为空");

            String apiType = dto.getApiType().trim().toUpperCase();
            String baseUrl = dto.getBaseUrl();
            if (baseUrl != null) {
                baseUrl = baseUrl.trim();
            }

            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(3000);
            requestFactory.setReadTimeout(5000);
            RestTemplate restTemplate = new RestTemplate(requestFactory);

            if ("OLLAMA".equals(apiType)) {
                String targetBaseUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : "http://localhost:11434";
                String url = OpenAiBaseUrlNormalizer.trimTrailingSlash(targetBaseUrl) + "/api/tags";
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, String.class);
                return response.getStatusCode().is2xxSuccessful();
            }

            // OPENAI / AZURE_OPENAI / CUSTOM 默认按 OpenAI 兼容接口验证
            String targetBaseUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : "https://api.openai.com";
            String normalized = OpenAiBaseUrlNormalizer.normalize(targetBaseUrl);
            String url = normalized + "/v1/models";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(dto.getApiKey().trim());
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException e) {
            return false;
        } catch (Exception e) {
            log.error("验证API配置失败", e);
            return false;
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * 保存/更新用户配置时的 baseUrl 规范化：避免出现 /v1/v1 这类重复路径导致调用 404。
     */
    private String normalizeBaseUrlForApiType(String apiType, String baseUrl) {
        if (baseUrl == null) {
            return null;
        }
        String trimmed = baseUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String type = apiType != null ? apiType.trim().toUpperCase() : "";
        if ("OPENAI".equals(type) || "AZURE_OPENAI".equals(type) || "CUSTOM".equals(type)) {
            return OpenAiBaseUrlNormalizer.normalize(trimmed);
        }
        return OpenAiBaseUrlNormalizer.trimTrailingSlash(trimmed);
    }

    @Override
    public UserApiConfig getSystemConfig() {
        // 查询系统配置 (user_id=0)
        UserApiConfig config = baseMapper.selectSystemConfig();
        if (config == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "系统未配置默认API，请先配置系统嵌入模型");
        }

        // 解密API Key
        config.setApiKey(AesUtil.decrypt(config.getApiKey()));

        return config;
    }

    @Override
    public String decryptApiKey(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.isEmpty()) {
            return null;
        }
        return AesUtil.decrypt(encryptedKey);
    }

    @Override
    public List<UserApiConfig> listByUserId(Long userId) {
        // 参数校验
        Assert.notNull(userId, "用户ID不能为空");

        // 查询用户的所有配置（不解密，不脱敏）
        LambdaQueryWrapper<UserApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserApiConfig::getUserId, userId)
                .eq(UserApiConfig::getStatus, 1)
                .orderByDesc(UserApiConfig::getIsDefault)
                .orderByDesc(UserApiConfig::getCreateTime);

        return this.list(wrapper);
    }
}
