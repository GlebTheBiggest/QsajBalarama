package com.qsaj.balarama.v1.repository;

import com.qsaj.balarama.v1.model.SavingsGoal;
import com.qsaj.balarama.v1.model.User;

import java.util.List;

public interface SavingsGoalRepository {
    void save(SavingsGoal goal);

    List<SavingsGoal> findAllByUser(User user);

    void delete(Long id);

    void update(SavingsGoal goal);

    SavingsGoal findById(Long id);
}