package com.dpbug.server.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dpbug.server.model.entity.user.UserOauth;
import org.apache.ibatis.annotations.Mapper;

/**
 * OAuth 绑定 Mapper
 *
 * @author dpbug
 */
@Mapper
public interface UserOauthMapper extends BaseMapper<UserOauth> {
}
