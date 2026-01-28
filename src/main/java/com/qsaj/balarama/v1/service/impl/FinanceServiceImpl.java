package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.Category;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.repository.TransactionRepository;
import com.qsaj.balarama.v1.repository.UserRepository;
import com.qsaj.balarama.v1.service.FinanceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FinanceServiceImpl implements FinanceService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Inject
    public FinanceServiceImpl(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void addTransaction(User user, String title, BigDecimal amount, TransactionType type, Category category, LocalDate date) {
        Transaction tx = new Transaction(title, amount, type, category, date, user);
        transactionRepository.save(tx);

        BigDecimal currentBalance = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;

        if (type == TransactionType.INCOME) {
            user.setSavedAmount(currentBalance.add(amount));
        } else {
            user.setSavedAmount(currentBalance.subtract(amount));
        }

        userRepository.update(user);
    }

    @Override
    public List<Transaction> getUserTransactions(User user) {
        return transactionRepository.findAllByUser(user);
    }

    @Override
    public void deleteTransaction(Long id, User user) {
        // 1. Знаходимо транзакцію, щоб знати суму і тип
        Transaction t = transactionRepository.findById(id); // Тобі треба додати findById в репозиторій!

        if (t != null) {
            // 2. Відкочуємо баланс
            BigDecimal currentBalance = user.getSavedAmount();

            if (t.getType() == TransactionType.INCOME) {
                // Якщо видаляємо дохід, то віднімаємо гроші у юзера
                user.setSavedAmount(currentBalance.subtract(t.getAmount()));
            } else {
                // Якщо видаляємо витрату, то повертаємо гроші юзеру
                user.setSavedAmount(currentBalance.add(t.getAmount()));
            }

            // 3. Оновлюємо юзера і видаляємо транзакцію
            userRepository.update(user);
            transactionRepository.delete(id);
        }
    }
}