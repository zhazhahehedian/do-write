package com.dpbug.server.service.oauth.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.dpbug.common.exception.BusinessException;
import com.dpbug.common.utils.AesUtil;
import com.dpbug.server.config.oauth.OAuthProperties;
import com.dpbug.server.model.dto.oauth.*;
import com.dpbug.server.model.entity.user.User;
import com.dpbug.server.model.entity.user.UserOauth;
import com.dpbug.server.model.enums.OAuthProviderEnum;
import com.dpbug.server.service.oauth.OAuthService;
import com.dpbug.server.service.user.UserOauthService;
import com.dpbug.server.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * OAuth 服务实现
 *
 * @author dpbug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final OAuthProperties oauthProperties;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final UserService userService;
    private final UserOauthService userOauthService;

    private static final String STATE_KEY_PREFIX = "oauth:state:";
    private static final String NONCE_KEY_PREFIX = "oauth:nonce:";

    // ==================== 公共方法 ====================

    @Override
    public OAuthLoginUrlVO getAuthorizeUrl(String provider) {
        OAuthProviderEnum providerEnum = OAuthProviderEnum.fromCode(provider);
        OAuthProperties.ProviderConfig config = oauthProperties.getProvider(provider);

        if (config.isOpenId()) {
            return buildOpenIdLoginUrl(provider, config, providerEnum);
        } else {
            return buildOAuth2LoginUrl(provider, config, providerEnum);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleOAuth2Callback(String provider, String code, String state) {
        // 1. 验证 state
        validateState(state, provider);

        OAuthProperties.ProviderConfig config = oauthProperties.getProvider(provider);

        // 2. 用 code 换取 access_token
        OAuthTokenResponse tokenResponse = exchangeToken(config, code);

        // 3. 获取用户信息
        LinuxdoUserInfo userInfo = getLinuxdoUserInfo(config, tokenResponse.getAccessToken());

        // 4. 检查账户是否被禁用
        if (userInfo.getActive() == null || !userInfo.getActive()) {
            throw new BusinessException("该账户已被禁用");
        }

        // 5. 查询或创建用户
        User user = findOrCreateUserFromLinuxdo(provider, userInfo, tokenResponse);

        // 6. 登录并返回 token
        StpUtil.login(user.getId());
        log.info("用户 {} 通过 {} OAuth 登录成功", user.getId(), provider);

        return StpUtil.getTokenValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleOpenIdCallback(String provider, Map<String, String> params) {
        OAuthProperties.ProviderConfig config = oauthProperties.getProvider(provider);

        // 1. 提取参数
        String identity = params.get("openid.identity");
        String responseNonce = params.get("openid.response_nonce");

        if (!StringUtils.hasText(identity)) {
            throw new BusinessException("缺少用户标识参数");
        }

        // 2. 检查 nonce 是否已使用（防重放）
        if (StringUtils.hasText(responseNonce)) {
            String nonceKey = NONCE_KEY_PREFIX + responseNonce;
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(
                nonceKey, "1", Duration.ofSeconds(config.getNonceExpiration())
            );
            if (isNew == null || !isNew) {
                throw new BusinessException("登录请求已过期或已被使用，请重新登录");
            }
        }

        // 3. 验证签名
        boolean isValid = verifyOpenIdSignature(config, params);
        if (!isValid) {
            throw new BusinessException("签名验证失败，登录请求无效");
        }

        // 4. 提取用户 ID
        String userId = extractUserIdFromIdentity(identity);
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("无法获取用户标识");
        }

        // 5. 获取用户信息
        FishpiUserInfo userInfo = getFishpiUserInfo(config, userId);

        // 6. 查询或创建用户
        User user = findOrCreateUserFromFishpi(provider, userInfo);

        // 7. 登录并返回 token
        StpUtil.login(user.getId());
        log.info("用户 {} 通过 {} OpenID 登录成功", user.getId(), provider);

        return StpUtil.getTokenValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long userId, String provider) {
        UserOauth userOauth = userOauthService.getByUserIdAndProvider(userId, provider);
        if (userOauth == null) {
            throw new BusinessException("未绑定该平台账号");
        }

        // 检查是否是唯一登录方式
        User user = userService.getById(userId);
        if (!StringUtils.hasText(user.getPassword())) {
            List<UserOauth> bindings = userOauthService.getByUserId(userId);
            if (bindings.size() <= 1) {
                throw new BusinessException("这是您唯一的登录方式，无法解绑");
            }
        }

        // 更新为解绑状态
        userOauth.setStatus(0);
        userOauth.setUnbindTime(LocalDateTime.now());
        userOauthService.updateById(userOauth);

        log.info("用户 {} 解绑 {} 成功", userId, provider);
    }

    @Override
    public List<UserOauth> getUserOAuthBindings(Long userId) {
        return userOauthService.getByUserId(userId);
    }

    // ==================== OAuth 2.0 私有方法 ====================

    /**
     * 构建 OAuth 2.0 授权 URL
     */
    private OAuthLoginUrlVO buildOAuth2LoginUrl(String provider,
            OAuthProperties.ProviderConfig config, OAuthProviderEnum providerEnum) {
        // 生成并存储 state
        String state = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
            STATE_KEY_PREFIX + state,
            provider,
            Duration.ofSeconds(oauthProperties.getStateExpiration())
        );

        // 构建授权 URL
        String scopeStr = config.getScopes() != null ? String.join(" ", config.getScopes()) : "";
        StringBuilder urlBuilder = new StringBuilder(config.getAuthorizeUrl());
        urlBuilder.append("?client_id=").append(config.getClientId());
        urlBuilder.append("&redirect_uri=").append(URLEncoder.encode(config.getRedirectUri(), StandardCharsets.UTF_8));
        urlBuilder.append("&response_type=code");
        if (StringUtils.hasText(scopeStr)) {
            urlBuilder.append("&scope=").append(URLEncoder.encode(scopeStr, StandardCharsets.UTF_8));
        }
        urlBuilder.append("&state=").append(state);

        return OAuthLoginUrlVO.builder()
            .authorizeUrl(urlBuilder.toString())
            .state(state)
            .provider(providerEnum.getName())
            .build();
    }

    /**
     * 验证 state 参数
     */
    private void validateState(String state, String provider) {
        String key = STATE_KEY_PREFIX + state;
        String cachedProvider = redisTemplate.opsForValue().get(key);

        if (cachedProvider == null) {
            throw new BusinessException("登录请求已过期，请重新登录");
        }

        if (!cachedProvider.equalsIgnoreCase(provider)) {
            throw new BusinessException("非法的登录请求");
        }

        // 验证通过后删除 state
        redisTemplate.delete(key);
    }

    /**
     * 用授权码换取 access_token
     */
    private OAuthTokenResponse exchangeToken(OAuthProperties.ProviderConfig config, String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", config.getClientId());
        params.add("client_secret", config.getClientSecret());
        params.add("code", code);
        params.add("redirect_uri", config.getRedirectUri());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<OAuthTokenResponse> response = restTemplate.postForEntity(
                config.getTokenUrl(),
                request,
                OAuthTokenResponse.class
            );

            if (response.getBody() == null || response.getBody().getAccessToken() == null) {
                throw new BusinessException("获取 access_token 失败");
            }

            return response.getBody();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("换取 access_token 失败", e);
            throw new BusinessException("登录失败，请重试");
        }
    }

    /**
     * 获取 Linux.do 用户信息
     */
    private LinuxdoUserInfo getLinuxdoUserInfo(OAuthProperties.ProviderConfig config, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<LinuxdoUserInfo> response = restTemplate.exchange(
                config.getUserInfoUrl(),
                HttpMethod.GET,
                request,
                LinuxdoUserInfo.class
            );

            if (response.getBody() == null || response.getBody().getId() == null) {
                throw new BusinessException("获取用户信息失败");
            }

            return response.getBody();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            throw new BusinessException("登录失败，请重试");
        }
    }

    // ==================== OpenID 2.0 私有方法（FishPi） ====================

    /**
     * 构建 OpenID 登录 URL（FishPi）
     */
    private OAuthLoginUrlVO buildOpenIdLoginUrl(String provider,
            OAuthProperties.ProviderConfig config, OAuthProviderEnum providerEnum) {
        // OpenID 固定参数
        String ns = "http://specs.openid.net/auth/2.0";
        String mode = "checkid_setup";
        String identitySelect = "http://specs.openid.net/auth/2.0/identifier_select";

        // 构建 OpenID 登录 URL
        StringBuilder urlBuilder = new StringBuilder(config.getLoginUrl());
        urlBuilder.append("?openid.ns=").append(URLEncoder.encode(ns, StandardCharsets.UTF_8));
        urlBuilder.append("&openid.mode=").append(mode);
        urlBuilder.append("&openid.return_to=").append(URLEncoder.encode(config.getReturnTo(), StandardCharsets.UTF_8));
        urlBuilder.append("&openid.realm=").append(URLEncoder.encode(config.getRealm(), StandardCharsets.UTF_8));
        urlBuilder.append("&openid.identity=").append(URLEncoder.encode(identitySelect, StandardCharsets.UTF_8));
        urlBuilder.append("&openid.claimed_id=").append(URLEncoder.encode(identitySelect, StandardCharsets.UTF_8));

        return OAuthLoginUrlVO.builder()
            .authorizeUrl(urlBuilder.toString())
            .state(null) // OpenID 不使用 state，使用 nonce
            .provider(providerEnum.getName())
            .build();
    }

    /**
     * 验证 OpenID 签名
     */
    private boolean verifyOpenIdSignature(OAuthProperties.ProviderConfig config, Map<String, String> params) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构造验证请求，将 mode 改为 check_authentication
            Map<String, String> verifyParams = new HashMap<>(params);
            verifyParams.put("openid.mode", "check_authentication");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(verifyParams, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                config.getVerifyUrl(),
                request,
                String.class
            );

            String body = response.getBody();
            if (body == null) {
                return false;
            }

            // 解析响应：格式为 ns:xxx\nis_valid:true/false
            return body.contains("is_valid:true");
        } catch (Exception e) {
            log.error("OpenID 签名验证失败", e);
            return false;
        }
    }

    /**
     * 从 identity URL 提取用户 ID
     * 例如：https://fishpi.cn/openid/id/123456 -> 123456
     */
    private String extractUserIdFromIdentity(String identity) {
        if (identity == null) {
            return null;
        }
        int lastSlash = identity.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < identity.length() - 1) {
            return identity.substring(lastSlash + 1);
        }
        return null;
    }

    /**
     * 获取 FishPi 用户信息
     */
    private FishpiUserInfo getFishpiUserInfo(OAuthProperties.ProviderConfig config, String userId) {
        try {
            String url = config.getUserInfoUrl() + "?userId=" + userId;
            log.info("请求 FishPi 用户信息: url={}", url);

            // 先获取原始响应用于调试
            ResponseEntity<String> rawResponse = restTemplate.getForEntity(url, String.class);
            log.info("FishPi 原始响应: {}", rawResponse.getBody());

            ResponseEntity<FishpiApiResponse> response = restTemplate.getForEntity(
                url,
                FishpiApiResponse.class
            );

            if (response.getBody() == null || response.getBody().getData() == null) {
                throw new BusinessException("获取用户信息失败");
            }

            FishpiUserInfo userInfo = response.getBody().getData();
            // FishPi 的响应中没有 oId，需要手动设置
            userInfo.setId(userId);

            if (userInfo.getUsername() == null) {
                throw new BusinessException("获取用户信息失败：用户名为空");
            }

            return userInfo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取 FishPi 用户信息失败", e);
            throw new BusinessException("登录失败，请重试");
        }
    }

    // ==================== 用户创建/更新方法 ====================

    /**
     * 从 Linux.do 用户信息查询或创建用户
     */
    private User findOrCreateUserFromLinuxdo(String provider, LinuxdoUserInfo userInfo,
            OAuthTokenResponse tokenResponse) {
        String oauthId = userInfo.getId().toString();

        // 1. 查询是否已绑定
        UserOauth existingBinding = userOauthService.getByProviderAndOauthId(provider, oauthId);

        if (existingBinding != null) {
            // 已绑定：更新 OAuth 信息并返回用户
            User user = userService.getById(existingBinding.getUserId());
            if (user == null || user.getStatus() != 1) {
                throw new BusinessException("账户已被禁用");
            }

            // 更新用户基本信息
            updateUserFromLinuxdo(user, userInfo);
            userService.updateById(user);

            // 更新 OAuth 绑定信息
            updateLinuxdoBinding(existingBinding, userInfo, tokenResponse);
            userOauthService.updateById(existingBinding);

            return user;
        }

        // 2. 未绑定：创建新用户并绑定
        User newUser = createUserFromLinuxdo(userInfo);
        createLinuxdoBinding(newUser.getId(), provider, userInfo, tokenResponse);

        return newUser;
    }

    /**
     * 从 FishPi 用户信息查询或创建用户
     */
    private User findOrCreateUserFromFishpi(String provider, FishpiUserInfo userInfo) {
        String oauthId = userInfo.getId();

        // 1. 查询是否已绑定
        UserOauth existingBinding = userOauthService.getByProviderAndOauthId(provider, oauthId);

        if (existingBinding != null) {
            // 已绑定：更新 OAuth 信息并返回用户
            User user = userService.getById(existingBinding.getUserId());
            if (user == null || user.getStatus() != 1) {
                throw new BusinessException("账户已被禁用");
            }

            // 更新用户基本信息
            updateUserFromFishpi(user, userInfo);
            userService.updateById(user);

            // 更新 OAuth 绑定信息
            updateFishpiBinding(existingBinding, userInfo);
            userOauthService.updateById(existingBinding);

            return user;
        }

        // 2. 未绑定：创建新用户并绑定
        User newUser = createUserFromFishpi(userInfo);
        createFishpiBinding(newUser.getId(), provider, userInfo);

        return newUser;
    }

    // ==================== Linux.do 用户相关方法 ====================

    private User createUserFromLinuxdo(LinuxdoUserInfo userInfo) {
        User user = new User();
        user.setId(IdUtil.getSnowflakeNextId());
        user.setUsername(generateUniqueUsername(userInfo.getUsername()));
        user.setNickname(StringUtils.hasText(userInfo.getName()) ? userInfo.getName() : userInfo.getUsername());
        user.setAvatar(userInfo.getAvatarUrl());
        user.setEmail(userInfo.getEmail());
        user.setStatus(1);
        user.setUserType("NORMAL");
        user.setLoginCount(1);
        user.setLastLoginTime(LocalDateTime.now());

        userService.save(user);
        log.info("创建新用户: id={}, username={}", user.getId(), user.getUsername());

        return user;
    }

    private void updateUserFromLinuxdo(User user, LinuxdoUserInfo userInfo) {
        if (StringUtils.hasText(userInfo.getName())) {
            user.setNickname(userInfo.getName());
        }
        if (StringUtils.hasText(userInfo.getAvatarUrl())) {
            user.setAvatar(userInfo.getAvatarUrl());
        }
        if (StringUtils.hasText(userInfo.getEmail())) {
            user.setEmail(userInfo.getEmail());
        }
        user.setLoginCount(user.getLoginCount() != null ? user.getLoginCount() + 1 : 1);
        user.setLastLoginTime(LocalDateTime.now());
    }

    private void createLinuxdoBinding(Long userId, String provider,
            LinuxdoUserInfo userInfo, OAuthTokenResponse tokenResponse) {
        UserOauth binding = new UserOauth();
        binding.setId(IdUtil.getSnowflakeNextId());
        binding.setUserId(userId);
        binding.setOauthType(provider.toUpperCase());
        binding.setOauthId(userInfo.getId().toString());
        binding.setOauthUserName(userInfo.getUsername());
        binding.setOauthNickname(userInfo.getName());
        binding.setOauthAvatar(userInfo.getAvatarUrl());
        binding.setOauthEmail(userInfo.getEmail());
        binding.setTrustLevel(userInfo.getTrustLevel());
        binding.setAccessToken(AesUtil.encrypt(tokenResponse.getAccessToken()));
        if (StringUtils.hasText(tokenResponse.getRefreshToken())) {
            binding.setRefreshToken(AesUtil.encrypt(tokenResponse.getRefreshToken()));
        }
        if (tokenResponse.getExpiresIn() != null) {
            binding.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));
        }
        binding.setStatus(1);
        binding.setBindTime(LocalDateTime.now());

        userOauthService.save(binding);
    }

    private void updateLinuxdoBinding(UserOauth binding, LinuxdoUserInfo userInfo,
            OAuthTokenResponse tokenResponse) {
        binding.setOauthUserName(userInfo.getUsername());
        binding.setOauthNickname(userInfo.getName());
        binding.setOauthAvatar(userInfo.getAvatarUrl());
        binding.setOauthEmail(userInfo.getEmail());
        binding.setTrustLevel(userInfo.getTrustLevel());
        binding.setAccessToken(AesUtil.encrypt(tokenResponse.getAccessToken()));
        if (StringUtils.hasText(tokenResponse.getRefreshToken())) {
            binding.setRefreshToken(AesUtil.encrypt(tokenResponse.getRefreshToken()));
        }
        if (tokenResponse.getExpiresIn() != null) {
            binding.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));
        }
    }

    // ==================== FishPi 用户相关方法 ====================

    private User createUserFromFishpi(FishpiUserInfo userInfo) {
        User user = new User();
        user.setId(IdUtil.getSnowflakeNextId());
        user.setUsername(generateUniqueUsername(userInfo.getUsername()));
        user.setNickname(StringUtils.hasText(userInfo.getNickname()) ? userInfo.getNickname() : userInfo.getUsername());
        user.setAvatar(userInfo.getAvatarUrl());
        user.setStatus(1);
        user.setUserType("NORMAL");
        user.setLoginCount(1);
        user.setLastLoginTime(LocalDateTime.now());

        userService.save(user);
        log.info("创建新用户(FishPi): id={}, username={}", user.getId(), user.getUsername());

        return user;
    }

    private void updateUserFromFishpi(User user, FishpiUserInfo userInfo) {
        if (StringUtils.hasText(userInfo.getNickname())) {
            user.setNickname(userInfo.getNickname());
        }
        if (StringUtils.hasText(userInfo.getAvatarUrl())) {
            user.setAvatar(userInfo.getAvatarUrl());
        }
        user.setLoginCount(user.getLoginCount() != null ? user.getLoginCount() + 1 : 1);
        user.setLastLoginTime(LocalDateTime.now());
    }

    private void createFishpiBinding(Long userId, String provider, FishpiUserInfo userInfo) {
        UserOauth binding = new UserOauth();
        binding.setId(IdUtil.getSnowflakeNextId());
        binding.setUserId(userId);
        binding.setOauthType(provider.toUpperCase());
        binding.setOauthId(userInfo.getId());
        binding.setOauthUserName(userInfo.getUsername());
        binding.setOauthNickname(userInfo.getNickname());
        binding.setOauthAvatar(userInfo.getAvatarUrl());
        // FishPi 的 OpenID 不返回 token，留空
        binding.setStatus(1);
        binding.setBindTime(LocalDateTime.now());

        userOauthService.save(binding);
    }

    private void updateFishpiBinding(UserOauth binding, FishpiUserInfo userInfo) {
        binding.setOauthUserName(userInfo.getUsername());
        binding.setOauthNickname(userInfo.getNickname());
        binding.setOauthAvatar(userInfo.getAvatarUrl());
    }

    // ==================== 通用工具方法 ====================

    /**
     * 生成唯一用户名
     */
    private String generateUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int suffix = 1;
        while (userService.getByUsername(username) != null) {
            username = baseUsername + "_" + suffix++;
        }
        return username;
    }
}
