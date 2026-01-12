package com.dpbug.server.model.dto.oauth;

import lombok.Builder;
import lombok.Data;

/**
 * OAuth 登录 URL 响应
 *
 * @author dpbug
 */
@Data
@Builder
public class OAuthLoginUrlVO {

    /**
     * 授权 URL
     */
    private String authorizeUrl;

    /**
     * state 参数（用于 CSRF 防护）
     */
    private String state;

    /**
     * 提供商名称
     */
    private String provider;
}
