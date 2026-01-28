package com.qsaj.balarama.v1.service;

import com.qsaj.balarama.v1.model.SavingsGoal;
import com.qsaj.balarama.v1.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface SavingsService {
    void createGoal(User user, String name, BigDecimal targetAmount);

    List<SavingsGoal> getUserGoals(User user);

    void addFundsToGoal(Long goalId, BigDecimal amount, User user);

    void deleteGoal(Long goalId, User user);
}