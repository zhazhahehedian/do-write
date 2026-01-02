package com.dpbug.server.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dpbug.server.model.entity.user.User;

public interface UserService extends IService<User> {
    /**
     * 获取用户名
     */
    User getByUsername(String userName);
}
