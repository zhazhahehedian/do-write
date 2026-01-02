package com.dpbug.server.model.entity.user;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dpbug.server.model.entity.base.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author dpbug
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends AuditableEntity {

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码（加密）
     */
    private String password;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 类型
     */
    private String userType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 登录次数
     */
    private Integer loginCount;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;
}
