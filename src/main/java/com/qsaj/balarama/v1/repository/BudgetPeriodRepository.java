package com.qsaj.balarama.v1.repository;

import com.qsaj.balarama.v1.model.BudgetPeriod;
import com.qsaj.balarama.v1.model.User;

import java.util.List;
import java.util.Optional;

public interface BudgetPeriodRepository {
    void save(BudgetPeriod period);
    void update(BudgetPeriod period);
    Optional<BudgetPeriod> findActiveByUser(User user);
    List<BudgetPeriod> findAllByUser(User user);
}