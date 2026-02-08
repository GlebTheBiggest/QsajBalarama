package com.qsaj.balarama.v1.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
public class DashboardStats {
    private final BigDecimal income;
    private final BigDecimal expense;
    private final Map<String, BigDecimal> expenseByCategory;

    public DashboardStats(BigDecimal income, BigDecimal expense, Map<String, BigDecimal> expenseByCategory) {
        this.income = income;
        this.expense = expense;
        this.expenseByCategory = expenseByCategory;
    }
}