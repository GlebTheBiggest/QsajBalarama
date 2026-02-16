package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.*;
import com.qsaj.balarama.v1.repository.TransactionRepository;
import com.qsaj.balarama.v1.repository.UserRepository;
import com.qsaj.balarama.v1.service.FinanceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FinanceServiceImpl implements FinanceService {
    private final TransactionRepository txRepo;
    private final UserRepository userRepo;

    @Inject
    public FinanceServiceImpl(TransactionRepository txRepo, UserRepository userRepo) {
        this.txRepo = txRepo;
        this.userRepo = userRepo;
    }

    @Override
    public void addTransaction(User user, String title, BigDecimal amount, TransactionType type,
                               Category category, LocalDate date, String notes,
                               LocalDate dueDate, TransactionStatus status, Priority priority) {
        Transaction tx = Transaction.builder()
                .title(title)
                .amount(amount)
                .type(type)
                .category(category)
                .date(date)
                .user(user)
                .notes(notes)
                .dueDate(dueDate)
                .status(status)
                .priority(priority)
                .build();

        txRepo.save(tx);

        // ВИПРАВЛЕНО: Баланс оновлюється для БУДЬ-ЯКОГО статусу (Completed, Pending, Overdue).
        // Якщо запис існує - гроші враховані.
        if (status != null) {
            applyBalance(user, amount, type, false);
        }
    }

    @Override
    public List<Transaction> getUserTransactions(User user) {
        return txRepo.findAllByUser(user);
    }

    @Override
    public void deleteTransaction(Long id, User user) {
        Transaction tx = txRepo.findById(id);
        if (tx == null) return;

        // ВИПРАВЛЕНО: При видаленні гроші повертаються незалежно від статусу.
        applyBalance(user, tx.getAmount(), tx.getType(), true);

        txRepo.delete(id);
    }

    private void applyBalance(User user, BigDecimal amount, TransactionType type, boolean reverse) {
        BigDecimal balance = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;

        BigDecimal result = (type == TransactionType.INCOME)
                ? (reverse ? balance.subtract(amount) : balance.add(amount))
                : (reverse ? balance.add(amount) : balance.subtract(amount));

        user.setSavedAmount(result);
        userRepo.update(user);
    }
}