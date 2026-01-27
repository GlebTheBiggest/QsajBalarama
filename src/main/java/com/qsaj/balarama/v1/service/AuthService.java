package com.qsaj.balarama.v1.service;

import com.qsaj.balarama.v1.model.User;

public interface AuthService {
    User register(String username, String email, String password);
    User authenticate(String username, String password);
}