package com.dpbug.server.service.auth;

import com.dpbug.server.model.entity.user.User;

public interface AuthService {

    void register(String username, String password, String email);

    String login(String username, String password);

    void logout();

    Long getCurrentUserId();

    User getCurrentUser();
}
