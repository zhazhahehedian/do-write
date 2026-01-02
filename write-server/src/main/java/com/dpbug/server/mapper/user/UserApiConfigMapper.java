package com.dpbug.server.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dpbug.server.model.entity.user.UserApiConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户API配置Mapper接口
 *
 * @author dpbug
 * @since 2025-12-27
 */
@Mapper
public interface UserApiConfigMapper extends BaseMapper<UserApiConfig> {

    /**
     * 查询用户的默认API配置
     *
     * @param userId 用户ID
     * @return 默认API配置
     */
    @Select("SELECT * FROM user_api_config " +
            "WHERE user_id = #{userId} AND is_default = 1 AND status = 1 " +
            "AND is_deleted = 0 LIMIT 1")
    UserApiConfig selectDefaultByUserId(@Param("userId") Long userId);

    /**
     * 查询系统配置 (user_id=0)
     *
     * @return 系统配置
     */
    @Select("SELECT * FROM user_api_config " +
            "WHERE user_id = 0 AND status = 1 AND is_deleted = 0 LIMIT 1")
    UserApiConfig selectSystemConfig();

    @Update("update user_api_config set is_default = 0 where user_id = #{userId} AND is_deleted = 0")
    void cancelDefaultByUserId(@Param("userId") Long userId);
}