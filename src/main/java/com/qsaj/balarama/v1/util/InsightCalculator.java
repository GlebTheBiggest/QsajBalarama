package com.qsaj.balarama.v1.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class InsightCalculator {
    private InsightCalculator() {}

    public static Insight calculate(BigDecimal balance, BigDecimal income, BigDecimal expense) {
        double progress = income.signum() > 0
                ? expense.divide(income, 2, RoundingMode.HALF_UP).doubleValue()
                : 1.0;

        if (balance.signum() < 0) {
            return new Insight("Negative Balance", "Cut expenses immediately.", progress);
        }

        if (progress > 0.9) {
            return new Insight("Budget Alert", "Almost all income spent.", progress);
        }

        return new Insight("Financial Health Good", "You're doing great.", progress);
    }
}