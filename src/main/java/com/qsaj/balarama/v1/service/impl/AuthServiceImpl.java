package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.Currency;
import com.qsaj.balarama.v1.repository.UserRepository;
import com.qsaj.balarama.v1.service.AuthService;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    @Inject
    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private static final String EMAIL_PATTERN = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

    @Override
    public void register(String username, String email, String password) {
        if (!email.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Invalid email format (e.g. user@mail.com)");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username is already taken!");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered!");
        }
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(username, hashedPassword, email);
        userRepository.save(user);
    }

    @Override
    public User authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();
        if (BCrypt.checkpw(password, user.getPassword())) {
            return user;
        } else {
            return null;
        }
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
        // Якщо юзернейм змінився, перевіряємо чи він вільний
        if (!user.getUsername().equals(newUsername) && userRepository.findByUsername(newUsername).isPresent()) {
            throw new IllegalArgumentException("Username already taken.");
        }
        // Якщо пошта змінилася, перевіряємо чи вона вільна
        if (!user.getEmail().equals(newEmail) && userRepository.findByEmail(newEmail).isPresent()) {
            throw new IllegalArgumentException("Email already taken.");
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