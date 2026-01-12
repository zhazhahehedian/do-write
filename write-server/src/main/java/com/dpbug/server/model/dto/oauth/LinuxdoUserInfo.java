package com.dpbug.server.model.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Linux.do OAuth 用户信息
 *
 * API 响应示例：
 * {
 *   "id": 123456,
 *   "username": "john_doe",
 *   "name": "John Doe",
 *   "avatar_url": "https://linux.do/user_avatar/...",
 *   "email": "john@example.com",
 *   "trust_level": 2,
 *   "active": true
 * }
 *
 * @author dpbug
 */
@Data
public class LinuxdoUserInfo {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("name")
    private String name;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("email")
    private String email;

    @JsonProperty("trust_level")
    private Integer trustLevel;

    @JsonProperty("active")
    private Boolean active;
}
