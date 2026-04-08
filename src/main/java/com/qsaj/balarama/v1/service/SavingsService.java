package com.qsaj.balarama.v1.service;

import com.qsaj.balarama.v1.model.SavingsGoal;
import com.qsaj.balarama.v1.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface SavingsService {
    void createGoal(User user, String name, BigDecimal targetAmount);

    // Додавання коштів з різних джерел
    void addFundsToGoalFromBudget(User user, Long goalId, BigDecimal amount);
    void addFundsToGoalFromUnallocated(User user, Long goalId, BigDecimal amount);

    // Перекази
    void transferFromGoalToUnallocated(User user, Long goalId, BigDecimal amount);
    void transferFromGoalToMain(User user, Long goalId, BigDecimal amount);
    void transferBetweenGoals(User user, Long fromGoalId, Long toGoalId, BigDecimal amount);

    // Завершення цілі
    void completeGoal(Long goalId, User user, String note);

    // Отримання списків
    List<SavingsGoal> getUserGoals(User user);
    List<SavingsGoal> getActiveGoals(User user);
    List<SavingsGoal> getCompletedGoals(User user);

    BigDecimal getTotalSavingsInGoals(User user);
    BigDecimal getTotalSavings(User user); // сума цілей + unallocated

    void deleteGoal(Long goalId, User user);

    // Допоміжний метод для переказу з unallocated на main (вже був)
    void transferFromUnallocatedToMain(User user, BigDecimal amount);
}