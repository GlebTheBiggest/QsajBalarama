package com.qsaj.balarama.v1.repository;

import com.qsaj.balarama.v1.model.BudgetPeriod;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.User;

import java.util.List;

public interface TransactionRepository {
    void save(Transaction transaction);
    List<Transaction> findAllByUser(User user);
    List<Transaction> findAllByBudgetPeriod(BudgetPeriod period); // НОВИЙ МЕТОД
    Transaction findById(Long id);
    void delete(Long id);
}