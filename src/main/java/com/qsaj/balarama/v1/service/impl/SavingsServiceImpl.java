package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.SavingsGoal;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.repository.SavingsGoalRepository;
import com.qsaj.balarama.v1.repository.UserRepository;
import com.qsaj.balarama.v1.service.SavingsService;

import java.math.BigDecimal;
import java.util.List;

public class SavingsServiceImpl implements SavingsService {

    private final SavingsGoalRepository savingsRepository;
    private final UserRepository userRepository;

    @Inject
    public SavingsServiceImpl(SavingsGoalRepository savingsRepository, UserRepository userRepository) {
        this.savingsRepository = savingsRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void createGoal(User user, String name, BigDecimal targetAmount) {
        SavingsGoal goal = new SavingsGoal(name, targetAmount, user);
        savingsRepository.save(goal);
    }

    @Override
    public List<SavingsGoal> getUserGoals(User user) {
        return savingsRepository.findAllByUser(user);
    }

    @Override
    public void addFundsToGoal(Long goalId, BigDecimal amount, User user) {
        // 1. Перевірка: чи є у юзера стільки грошей на балансі?
        BigDecimal currentBalance = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;

        if (currentBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Not enough funds in your main balance!");
        }

        SavingsGoal goal = savingsRepository.findById(goalId);
        if (goal == null) throw new IllegalArgumentException("Goal not found");

        // 2. Трансфер
        user.setSavedAmount(currentBalance.subtract(amount)); // Знімаємо з гаманця
        goal.setCurrentAmount(goal.getCurrentAmount().add(amount)); // Кладемо в скарбничку

        // 3. Зберігаємо
        userRepository.update(user);
        savingsRepository.update(goal);
    }

    @Override
    public void deleteGoal(Long goalId, User user) {
        SavingsGoal goal = savingsRepository.findById(goalId);
        if (goal != null) {
            // Повертаємо накопичені гроші назад у гаманець!
            BigDecimal saved = goal.getCurrentAmount();
            if (saved.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentBalance = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;
                user.setSavedAmount(currentBalance.add(saved));
                userRepository.update(user);
            }
            savingsRepository.delete(goalId);
        }
    }
}