package com.dpbug.server.model.entity.user;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dpbug.server.model.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_oauth")
public class UserOauth extends BaseEntity {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 第三方类型
     */
    private String oauthType;

    /**
     * 第三方唯一标识
     */
    private String oauthId;

    /**
     * 第三方平台用户名
     */
    private String oauthUserName;

    /**
     * 第三方头像
     */
    private String oauthAvatar;

    /**
     * 第三方平台邮箱
     */
    private String oauthEmail;

    /**
     * 令牌
     */
    private String accessToken;
    private String refreshToken;
    private LocalDateTime expiresAt;

    /**
     * 绑定状态
     */
    private Integer status;

    /**
     * 绑定时间
     */
    private LocalDateTime bindTime;

    /**
     * 解绑时间
     */
    private LocalDateTime unbindTime;
}
