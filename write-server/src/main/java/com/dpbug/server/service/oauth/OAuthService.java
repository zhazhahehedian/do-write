package com.dpbug.server.service.oauth;

import com.dpbug.server.model.dto.oauth.OAuthLoginUrlVO;
import com.dpbug.server.model.entity.user.UserOauth;

import java.util.List;
import java.util.Map;

/**
 * OAuth 服务接口
 *
 * @author dpbug
 */
public interface OAuthService {

    /**
     * 生成授权 URL
     *
     * @param provider 提供商代码（如 linuxdo, fishpi）
     * @return 包含授权 URL 和 state 的响应
     */
    OAuthLoginUrlVO getAuthorizeUrl(String provider);

    /**
     * 处理 OAuth 2.0 回调（Linux.do, GitHub）
     *
     * @param provider 提供商代码
     * @param code     授权码
     * @param state    state 参数
     * @return sa-token 令牌
     */
    String handleOAuth2Callback(String provider, String code, String state);

    /**
     * 处理 OpenID 2.0 回调（FishPi）
     *
     * @param provider 提供商代码
     * @param params   回调参数（openid.* 参数）
     * @return sa-token 令牌
     */
    String handleOpenIdCallback(String provider, Map<String, String> params);

    /**
     * 解绑 OAuth
     *
     * @param userId   用户ID
     * @param provider 提供商代码
     */
    void unbind(Long userId, String provider);

    /**
     * 获取用户的 OAuth 绑定列表
     *
     * @param userId 用户ID
     * @return 绑定列表
     */
    List<UserOauth> getUserOAuthBindings(Long userId);
}
