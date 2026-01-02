package com.dpbug.server.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.dpbug.server.model.entity.user.User;
import com.dpbug.server.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * sa-token 权限接口实现
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserService userService;


    /**
     * 返回用户权限列表,目前不考虑权限
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    /**
     * 返回用户角色列表
     * 本项目只有两种角色：NORMAL、ADMIN
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = userService.getById((Long) loginId);
        if (user == null) {
            return Collections.emptyList();
        }
        // 返回用户类型作为角色
        return Collections.singletonList(user.getUserType());
    }
}
