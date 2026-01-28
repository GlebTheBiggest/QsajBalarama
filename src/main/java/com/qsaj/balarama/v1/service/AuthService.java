package com.qsaj.balarama.v1.service;

import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.Currency;

public interface AuthService {
    void register(String username, String email, String password);

    User authenticate(String username, String password);

    void changePassword(User user, String currentPassword, String newPassword);

    void updateProfile(User user, String newUsername, String newEmail);

    boolean checkPassword(User user, String rawPassword);

    void updateCurrency(User user, Currency currency);
}