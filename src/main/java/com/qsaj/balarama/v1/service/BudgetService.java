package com.qsaj.balarama.v1.service;

import com.qsaj.balarama.v1.model.BudgetPeriod;
import com.qsaj.balarama.v1.model.CategoryBudget;
import com.qsaj.balarama.v1.model.CategoryGroup;
import com.qsaj.balarama.v1.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BudgetService {
    BudgetPeriod getActivePeriod(User user);
    BudgetPeriod startNewPeriod(User user, LocalDate startDate, LocalDate endDate);
    void recalculateSpentAmounts(BudgetPeriod period);
    void closeActivePeriod(User user);
    void setCategoryBudget(BudgetPeriod period, CategoryGroup group, BigDecimal amount);
    void removeCategoryBudget(BudgetPeriod period, CategoryGroup group);
    List<CategoryBudget> getBudgetsForPeriod(BudgetPeriod period);
    void updatePeriod(BudgetPeriod period);
    List<BudgetPeriod> getAllPeriods(User user);
}