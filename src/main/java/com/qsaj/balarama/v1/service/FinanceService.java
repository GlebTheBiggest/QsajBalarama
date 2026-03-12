package com.qsaj.balarama.v1.service;

import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.CategoryItem;
import com.qsaj.balarama.v1.model.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FinanceService {
    void addTransaction(User user, String title, BigDecimal amount, TransactionType type,
                        CategoryItem categoryItem, LocalDate date, String notes,
                        LocalDate dueDate, TransactionStatus status, Priority priority);
    List<Transaction> getUserTransactions(User user);
    void deleteTransaction(Long id, User user);
}