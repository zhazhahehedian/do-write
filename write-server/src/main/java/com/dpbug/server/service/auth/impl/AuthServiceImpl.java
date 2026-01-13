package com.dpbug.server.service.auth.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.dpbug.common.utils.Assert;
import com.dpbug.server.model.entity.user.User;
import com.dpbug.server.service.auth.AuthService;
import com.dpbug.server.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 注册
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * TODO 邮箱验证码对接
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String username, String password, String email) {
        // 1. 校验用户名是否已存在
        Assert.isNull(userService.getByUsername(username), "用户名已存在");

        // 2. 创建用户
        User user = new User();
        user.setUsername(username);
        user.setNickname(username);  // 默认昵称使用用户名
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setUserType("NORMAL");
        user.setStatus(1);

        userService.save(user);
    }

    /**
     * 登录
     * @param username 用户名
     * @param password 密码
     * @return token
     * TODO 邮箱+密码也可以登录
     */
    @Override
    public String login(String username, String password) {
        User user = userService.getByUsername(username);
        Assert.notNull(user, "用户不存在");
        Assert.isTrue(user.getStatus() == 1, "账号已被禁用");

        Assert.isTrue(passwordEncoder.matches(password, user.getPassword()), "密码错误");

        // 登录生成token并存入redis
        StpUtil.login(user.getId());
        // 缓存信息到session
        StpUtil.getSession().set("user", user);

        return StpUtil.getTokenValue();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public User getCurrentUser() {
        // 先从Session获取
        User user = (User) StpUtil.getSession().get("user");
        if (user != null) {
            return user;
        }
        // Session 中没有，从数据库查询并缓存
        Long userId = getCurrentUserId();
        user = userService.getById(userId);
        StpUtil.getSession().set("user", user);

        return user;
    }

}
