package com.dpbug.server.controller.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.dpbug.common.domain.Result;
import com.dpbug.server.config.oauth.OAuthProperties;
import com.dpbug.server.model.dto.oauth.OAuthLoginUrlVO;
import com.dpbug.server.model.entity.user.UserOauth;
import com.dpbug.server.service.oauth.OAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OAuth 登录控制器
 *
 * @author dpbug
 */
@Slf4j
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oauthService;
    private final OAuthProperties oauthProperties;

    @Value("${oauth.frontend-callback-url:http://localhost:3000/oauth/callback}")
    private String frontendCallbackUrl;

    /**
     * 获取授权 URL
     * GET /api/oauth/{provider}/authorize
     */
    @GetMapping("/{provider}/authorize")
    public Result<OAuthLoginUrlVO> getAuthorizeUrl(@PathVariable String provider) {
        log.info("获取 {} OAuth 授权 URL", provider);
        OAuthLoginUrlVO result = oauthService.getAuthorizeUrl(provider);
        return Result.success(result);
    }

    /**
     * OAuth 2.0 / OpenID 2.0 回调处理
     * GET /api/oauth/{provider}/callback
     *
     * 处理完成后重定向到前端页面
     */
    @GetMapping("/{provider}/callback")
    public void callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam Map<String, String> allParams,
            HttpServletResponse response) throws IOException {

        log.info("处理 {} 回调", provider);

        try {
            OAuthProperties.ProviderConfig config = oauthProperties.getProvider(provider);
            String token;

            if (config.isOpenId()) {
                // OpenID 2.0 回调（FishPi）
                Map<String, String> openIdParams = allParams.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("openid."))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                token = oauthService.handleOpenIdCallback(provider, openIdParams);
            } else {
                // OAuth 2.0 回调（Linux.do, GitHub）
                token = oauthService.handleOAuth2Callback(provider, code, state);
            }

            // 重定向到前端回调页面，带上 token
            String redirectUrl = frontendCallbackUrl + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            log.info("OAuth 登录成功，重定向到: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth 回调处理失败", e);
            // 重定向到前端回调页面，带上错误信息
            String errorMessage = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            String redirectUrl = frontendCallbackUrl + "?error=" + errorMessage;
            response.sendRedirect(redirectUrl);
        }
    }

    /**
     * 解绑 OAuth（需要登录）
     * POST /api/oauth/unbind?provider=xxx
     */
    @PostMapping("/unbind")
    public Result<Void> unbind(@RequestParam String provider) {
        Long userId = StpUtil.getLoginIdAsLong();
        oauthService.unbind(userId, provider);
        return Result.success();
    }

    /**
     * 获取用户 OAuth 绑定列表（需要登录）
     * GET /api/oauth/bindings
     */
    @GetMapping("/bindings")
    public Result<List<UserOauth>> getBindings() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<UserOauth> bindings = oauthService.getUserOAuthBindings(userId);
        return Result.success(bindings);
    }
}
