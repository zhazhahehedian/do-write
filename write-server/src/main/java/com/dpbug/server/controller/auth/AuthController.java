package com.dpbug.server.controller.auth;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.dpbug.common.domain.Result;
import com.dpbug.server.model.entity.user.User;
import com.dpbug.server.service.auth.AuthService;
import com.dpbug.server.model.dto.auth.LoginRequest;
import com.dpbug.server.model.dto.auth.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest request) {
        authService.register(
                request.getUsername(),
                request.getPassword(),
                request.getEmail()
        );
        return Result.success();
    }

    /**
     * 账号密码登录
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());
        return Result.success(token);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    @SaCheckLogin
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user/info")
    @SaCheckLogin
    public Result<User> getUserInfo() {
        User user = authService.getCurrentUser();
        return Result.success(user);
    }
}
