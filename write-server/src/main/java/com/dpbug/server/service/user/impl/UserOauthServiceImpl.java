package com.dpbug.server.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dpbug.server.mapper.user.UserOauthMapper;
import com.dpbug.server.model.entity.user.UserOauth;
import com.dpbug.server.service.user.UserOauthService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OAuth 绑定服务实现
 *
 * @author dpbug
 */
@Service
public class UserOauthServiceImpl extends ServiceImpl<UserOauthMapper, UserOauth> implements UserOauthService {

    @Override
    public UserOauth getByUserIdAndProvider(Long userId, String provider) {
        return getOne(new LambdaQueryWrapper<UserOauth>()
                .eq(UserOauth::getUserId, userId)
                .eq(UserOauth::getOauthType, provider.toUpperCase())
                .eq(UserOauth::getStatus, 1));
    }

    @Override
    public UserOauth getByProviderAndOauthId(String provider, String oauthId) {
        return getOne(new LambdaQueryWrapper<UserOauth>()
                .eq(UserOauth::getOauthType, provider.toUpperCase())
                .eq(UserOauth::getOauthId, oauthId)
                .eq(UserOauth::getStatus, 1));
    }

    @Override
    public List<UserOauth> getByUserId(Long userId) {
        return list(new LambdaQueryWrapper<UserOauth>()
                .eq(UserOauth::getUserId, userId)
                .eq(UserOauth::getStatus, 1));
    }
}
