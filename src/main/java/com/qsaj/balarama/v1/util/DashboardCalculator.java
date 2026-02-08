package com.qsaj.balarama.v1.util;

import com.qsaj.balarama.v1.dto.DashboardStats;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.enums.TransactionStatus;
import com.qsaj.balarama.v1.model.enums.TransactionType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DashboardCalculator {
    private DashboardCalculator() {}

    public static DashboardStats calculate(List<Transaction> transactions) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        Map<String, BigDecimal> expenseByCategory = new HashMap<>();

        for (Transaction t : transactions) {
            if (t.getStatus() != TransactionStatus.COMPLETED) {
                continue;
            }

            if (t.getType() == TransactionType.INCOME) {
                income = income.add(t.getAmount());
            } else {
                expense = expense.add(t.getAmount());
                String category = t.getCategory().name();
                expenseByCategory.merge(category, t.getAmount(), BigDecimal::add);
            }
        }

        return new DashboardStats(income, expense, expenseByCategory);
    }
}