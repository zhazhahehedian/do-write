package com.dpbug.server.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dpbug.server.model.entity.user.UserOauth;

import java.util.List;

/**
 * OAuth 绑定服务接口
 *
 * @author dpbug
 */
public interface UserOauthService extends IService<UserOauth> {

    /**
     * 根据用户ID和提供商查询绑定
     *
     * @param userId   用户ID
     * @param provider 提供商代码
     * @return 绑定信息
     */
    UserOauth getByUserIdAndProvider(Long userId, String provider);

    /**
     * 根据提供商和OAuth ID查询绑定
     *
     * @param provider 提供商代码
     * @param oauthId  OAuth用户ID
     * @return 绑定信息
     */
    UserOauth getByProviderAndOauthId(String provider, String oauthId);

    /**
     * 根据用户ID查询所有绑定
     *
     * @param userId 用户ID
     * @return 绑定列表
     */
    List<UserOauth> getByUserId(Long userId);
}
