package com.qsaj.balarama.v1.repository;

import com.qsaj.balarama.v1.model.BudgetPeriod;
import com.qsaj.balarama.v1.model.CategoryBudget;
import com.qsaj.balarama.v1.model.CategoryGroup;

import java.util.List;
import java.util.Optional;

public interface CategoryBudgetRepository {
    void save(CategoryBudget budget);
    void update(CategoryBudget budget);
    List<CategoryBudget> findByPeriod(BudgetPeriod period);
    Optional<CategoryBudget> findByPeriodAndGroup(BudgetPeriod period, CategoryGroup group);
}