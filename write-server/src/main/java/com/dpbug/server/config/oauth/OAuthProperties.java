package com.dpbug.server.config.oauth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OAuth 配置属性类
 *
 * @author dpbug
 */
@Data
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    /**
     * State 过期时间（秒）
     */
    private Long stateExpiration = 600L;

    /**
     * OAuth 提供商配置
     */
    private Map<String, ProviderConfig> providers = new HashMap<>();

    @Data
    public static class ProviderConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = true;

        /**
         * 认证类型：oauth2, openid
         */
        private String type = "oauth2";

        // ========== OAuth 2.0 配置 ==========
        private String clientId;
        private String clientSecret;
        private String authorizeUrl;
        private String tokenUrl;
        private String userInfoUrl;
        private String redirectUri;
        private List<String> scopes;

        // ========== OpenID 2.0 配置（FishPi） ==========
        private String loginUrl;
        private String verifyUrl;
        private String returnTo;
        private String realm;
        private Long nonceExpiration = 300L;

        /**
         * 是否为 OpenID 类型
         */
        public boolean isOpenId() {
            return "openid".equalsIgnoreCase(type);
        }

        /**
         * 是否为 OAuth2 类型
         */
        public boolean isOAuth2() {
            return "oauth2".equalsIgnoreCase(type);
        }
    }

    /**
     * 获取指定提供商配置
     *
     * @param name 提供商名称
     * @return 提供商配置
     */
    public ProviderConfig getProvider(String name) {
        ProviderConfig config = providers.get(name.toLowerCase());
        if (config == null) {
            throw new IllegalArgumentException("OAuth provider not found: " + name);
        }
        if (!config.getEnabled()) {
            throw new IllegalArgumentException("OAuth provider is disabled: " + name);
        }
        return config;
    }

    /**
     * 检查提供商是否存在且启用
     *
     * @param name 提供商名称
     * @return 是否可用
     */
    public boolean isProviderAvailable(String name) {
        ProviderConfig config = providers.get(name.toLowerCase());
        return config != null && config.getEnabled();
    }
}
