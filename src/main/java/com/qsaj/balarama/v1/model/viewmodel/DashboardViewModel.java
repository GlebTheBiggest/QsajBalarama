package com.qsaj.balarama.v1.model.viewmodel;

import com.qsaj.balarama.v1.dto.DashboardStats;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.Currency;
import com.qsaj.balarama.v1.service.FinanceService;
import com.qsaj.balarama.v1.util.DashboardCalculator;
import javafx.beans.property.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

public class DashboardViewModel {

    private final FinanceService financeService;
    private final User user;

    // --- STATE ---
    private final StringProperty balance = new SimpleStringProperty();
    private final StringProperty income = new SimpleStringProperty();
    private final StringProperty expense = new SimpleStringProperty();

    private final StringProperty insightTitle = new SimpleStringProperty();
    private final StringProperty insightText = new SimpleStringProperty();
    private final DoubleProperty budgetProgress = new SimpleDoubleProperty();
    private final StringProperty budgetPercent = new SimpleStringProperty();

    private final ObjectProperty<YearMonth> currentMonth =
            new SimpleObjectProperty<>(YearMonth.now());

    @Getter
    private List<Transaction> transactions;

    public DashboardViewModel(User user, FinanceService financeService) {
        this.user = user;
        this.financeService = financeService;
    }

    public void load() {
        Currency currency = user.getCurrency() != null ? user.getCurrency() : Currency.USD;
        String symbol = currency.getSymbol();

        transactions = financeService.getUserTransactions(user);
        DashboardStats stats = DashboardCalculator.calculate(transactions);

        BigDecimal balanceValue =
                user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;

        balance.set(symbol + " " + balanceValue);
        income.set("+ " + symbol + " " + stats.getIncome());
        expense.set("- " + symbol + " " + stats.getExpense());

        calculateInsight(balanceValue, stats.getIncome(), stats.getExpense());
    }

    private void calculateInsight(BigDecimal balanceVal, BigDecimal incomeVal, BigDecimal expenseVal) {
        double progress = incomeVal.compareTo(BigDecimal.ZERO) > 0
                ? expenseVal.divide(incomeVal, 2, RoundingMode.HALF_UP).doubleValue()
                : 1.0;

        budgetProgress.set(Math.min(progress, 1));
        budgetPercent.set((int) (progress * 100) + "%");

        if (balanceVal.signum() < 0) {
            insightTitle.set("Negative Balance");
            insightText.set("Cut expenses immediately.");
        } else if (progress > 0.9) {
            insightTitle.set("Budget Alert");
            insightText.set("Almost all income spent.");
        } else {
            insightTitle.set("Financial Health Good");
            insightText.set("You're doing great.");
        }
    }

    // --- GETTERS FOR BINDING ---

    public StringProperty balanceProperty() { return balance; }
    public StringProperty incomeProperty() { return income; }
    public StringProperty expenseProperty() { return expense; }

    public StringProperty insightTitleProperty() { return insightTitle; }
    public StringProperty insightTextProperty() { return insightText; }

    public DoubleProperty budgetProgressProperty() { return budgetProgress; }
    public StringProperty budgetPercentProperty() { return budgetPercent; }

    public ObjectProperty<YearMonth> currentMonthProperty() { return currentMonth; }

}
