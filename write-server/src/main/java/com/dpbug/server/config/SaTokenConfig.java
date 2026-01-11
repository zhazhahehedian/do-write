package com.dpbug.server.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * sa-token 配置类
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，处理异步dispatch时跳过验证
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 添加登录认证路由及排除
            SaRouter.match("/**")
                    .notMatch("/api/auth/login")              // 登录接口
                    .notMatch("/api/auth/register")           // 注册接口
                    .notMatch("/api/oauth/**")                // OAuth 接口
                    .notMatch("/error")                       // 错误页面
                    .notMatch("/favicon.ico")                 // 网站图标
                    .check(r -> StpUtil.checkLogin());     // 登录校验

            // 管理员接口需要角色校验
            SaRouter.match("/api/admin/**")
                    .check(r -> StpUtil.checkRole("ADMIN"));
        }) {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                // 跳过非初始请求的dispatch（ERROR/ASYNC/FORWARD场景）
                // 这些dispatch类型不经过SaToken的filter，没有上下文
                String dispatcherType = request.getDispatcherType().name();
                if ("ASYNC".equals(dispatcherType) || "ERROR".equals(dispatcherType) || "FORWARD".equals(dispatcherType)) {
                    return true;
                }
                return super.preHandle(request, response, handler);
            }
        }).addPathPatterns("/**");
    }
}
