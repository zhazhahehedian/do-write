package com.dpbug.server.model.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * FishPi（摸鱼派）用户信息
 *
 * API: GET https://fishpi.cn/api/user/getInfoById?userId=xxx
 *
 * @author dpbug
 */
@Data
public class FishpiUserInfo {

    /**
     * 用户ID（oId）
     */
    @JsonProperty("oId")
    private String id;

    /**
     * 用户名
     */
    @JsonProperty("userName")
    private String username;

    /**
     * 昵称
     */
    @JsonProperty("userNickname")
    private String nickname;

    /**
     * 头像 URL
     */
    @JsonProperty("userAvatarURL")
    private String avatarUrl;
}
