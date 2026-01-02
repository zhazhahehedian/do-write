package com.dpbug.server.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dpbug.server.model.entity.user.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 *
 * @author dpbug
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}