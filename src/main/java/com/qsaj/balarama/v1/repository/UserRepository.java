package com.qsaj.balarama.v1.repository;

import com.qsaj.balarama.v1.model.User;

import java.util.Optional;

public interface UserRepository {
    void save(User user);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    void update(User user);

}