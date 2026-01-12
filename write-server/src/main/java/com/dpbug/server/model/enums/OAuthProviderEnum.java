package com.dpbug.server.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OAuth 提供商枚举
 *
 * @author dpbug
 */
@Getter
@RequiredArgsConstructor
public enum OAuthProviderEnum {

    LINUXDO("linuxdo", "Linux.do"),
    FISHPI("fishpi", "摸鱼派"),
    GITHUB("github", "GitHub");

    private final String code;
    private final String name;

    /**
     * 根据 code 获取枚举
     *
     * @param code 提供商代码
     * @return 枚举值
     */
    public static OAuthProviderEnum fromCode(String code) {
        for (OAuthProviderEnum provider : values()) {
            if (provider.getCode().equalsIgnoreCase(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown OAuth provider: " + code);
    }
}
