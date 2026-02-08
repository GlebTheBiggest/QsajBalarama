package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.Currency;
import com.qsaj.balarama.v1.repository.UserRepository;
import com.qsaj.balarama.v1.service.AuthService;
import com.qsaj.balarama.v1.util.ValidationUtil;
import org.mindrot.jbcrypt.BCrypt;

public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;

    @Inject
    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void register(String username, String email, String password) {
        ValidationUtil.validateEmail(email);

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username is already taken!");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered!");
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = User.builder()
                .username(username)
                .password(hashedPassword)
                .email(email)
                .build();
        userRepository.save(user);
    }

    @Override
    public User authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> BCrypt.checkpw(password, user.getPassword()))
                .orElse(null);
    }

    @Override
    public boolean checkPassword(User user, String rawPassword) {
        return BCrypt.checkpw(rawPassword, user.getPassword());
    }

    @Override
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!checkPassword(user, currentPassword)) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        user.setPassword(hashedPassword);
        userRepository.update(user);
    }

    @Override
    public void updateProfile(User user, String newUsername, String newEmail) {
        ValidationUtil.validateEmail(newEmail);

        if (!user.getUsername().equals(newUsername)) {
            userRepository.findByUsername(newUsername)
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Username already taken.");
                    });
        }

        if (!user.getEmail().equals(newEmail)) {
            userRepository.findByEmail(newEmail)
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Email already taken.");
                    });
        }

        user.setUsername(newUsername);
        user.setEmail(newEmail);
        userRepository.update(user);
    }

    @Override
    public void updateCurrency(User user, Currency currency) {
        user.setCurrency(currency);
        userRepository.update(user);
    }
}