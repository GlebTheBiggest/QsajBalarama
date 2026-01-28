package com.qsaj.balarama.v1.service;

import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.Category;
import com.qsaj.balarama.v1.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FinanceService {
    void addTransaction(User user, String title, BigDecimal amount, TransactionType type, Category category, LocalDate date);

    List<Transaction> getUserTransactions(User user);

    void deleteTransaction(Long id, User user);
}