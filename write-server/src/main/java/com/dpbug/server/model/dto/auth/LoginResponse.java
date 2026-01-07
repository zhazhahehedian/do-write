package com.dpbug.server.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * 访问令牌
     */
    private String token;

    /**
     * 令牌类型
     */
    private String tokenType = "Bearer";

    public LoginResponse(String token) {
        this.token = token;
        this.tokenType = "Bearer";
    }

    public static LoginResponse of(String token) {
        return new LoginResponse(token);
    }
}
