package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.Currency;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.service.FinanceService;
import com.qsaj.balarama.v1.session.UserSession;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;
    @FXML private Label expenseLabel;
    @FXML private Label incomeLabel;

    @FXML private VBox recentTransactionsContainer;

    // Нові поля для Smart Insight
    @FXML private Label insightTitleLabel;
    @FXML private Label insightTextLabel;
    @FXML private ProgressBar budgetProgressBar;
    @FXML private Label budgetPercentLabel;

    private final UserSession userSession;
    private final FinanceService financeService;

    @Inject
    public DashboardController(UserSession userSession, FinanceService financeService) {
        this.userSession = userSession;
        this.financeService = financeService;
    }

    @FXML
    public void initialize() {
        refreshDashboard();
    }

    private void refreshDashboard() {
        User user = userSession.getCurrentUser();
        Currency currency = (user.getCurrency() != null) ? user.getCurrency() : Currency.USD;
        String symbol = currency.getSymbol();

        // --- ЗМІНА ТУТ ---
        welcomeLabel.setText("Hi, " + user.getUsername() + "!");
        welcomeLabel.getStyleClass().add("welcome-text");

        List<Transaction> transactions = financeService.getUserTransactions(user);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(t.getAmount());
            } else {
                totalExpense = totalExpense.add(t.getAmount());
            }
        }

        BigDecimal currentBalance = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;

        balanceLabel.setText(symbol + " " + currentBalance);
        incomeLabel.setText("+ " + symbol + " " + totalIncome);
        expenseLabel.setText("- " + symbol + " " + totalExpense);

        // --- ГЕНЕРАЦІЯ ІНСАЙТІВ (ЛОГІКА) ---
        generateSmartInsight(currentBalance, totalIncome, totalExpense, symbol);

        // Список транзакцій
        recentTransactionsContainer.getChildren().clear();
        if (transactions.isEmpty()) {
            Label placeholder = new Label("No transactions yet.");
            placeholder.setStyle("-fx-text-fill: #95a5a6; -fx-padding: 10;");
            recentTransactionsContainer.getChildren().add(placeholder);
        } else {
            int limit = Math.min(transactions.size(), 5);
            for (int i = 0; i < limit; i++) {
                Transaction t = transactions.get(i);
                recentTransactionsContainer.getChildren().add(createTransactionRow(t, symbol));
            }
        }
    }

    private void generateSmartInsight(BigDecimal balance, BigDecimal income, BigDecimal expense, String symbol) {
        // 1. Прогрес бар бюджету (Витрати / Дохід)
        double progress = 0.0;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            progress = expense.divide(income, 2, RoundingMode.HALF_UP).doubleValue();
        } else if (expense.compareTo(BigDecimal.ZERO) > 0) {
            progress = 1.0; // Якщо доходу 0, а витрати є - це 100% біда
        }

        // Обмежимо максимум 1.0 для бару
        budgetProgressBar.setProgress(Math.min(progress, 1.0));
        budgetPercentLabel.setText((int)(progress * 100) + "%");

        // Змінюємо колір бару
        budgetProgressBar.setStyle("-fx-accent: " + (progress > 0.9 ? "#e74c3c" : "#27ae60") + ";");

        // 2. Розумний текст
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            insightTitleLabel.setText("Warning: Negative Balance");
            insightTextLabel.setText("Your balance is negative (" + symbol + balance + "). Try to cut non-essential expenses immediately.");
            insightTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c;"); // Червоний

        } else if (progress > 0.9) {
            insightTitleLabel.setText("Budget Alert");
            insightTextLabel.setText("You have spent over 90% of your income. Watch out for the rest of the month!");
            insightTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22;"); // Оранжевий

        } else if (progress < 0.5 && income.compareTo(BigDecimal.ZERO) > 0) {
            insightTitleLabel.setText("Excellent Saving");
            insightTextLabel.setText("You saved more than 50% of your income this period. Consider investing the surplus.");
            insightTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;"); // Зелений

        } else if (income.compareTo(BigDecimal.ZERO) == 0 && expense.compareTo(BigDecimal.ZERO) == 0) {
            insightTitleLabel.setText("Let's Get Started");
            insightTextLabel.setText("Add your first income or expense to see analytics here.");
            insightTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-primary;");

        } else {
            insightTitleLabel.setText("Stable Financials");
            insightTextLabel.setText("Your spending is within normal limits. You have " + symbol + balance + " available.");
            insightTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-primary;");
        }
    }

    private HBox createTransactionRow(Transaction t, String currencySymbol) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(15);
        row.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 2, 0, 0, 1);");

        VBox metaBox = new VBox();
        metaBox.setSpacing(3);

        Label titleLbl = new Label(t.getTitle());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-header; -fx-font-size: 13px;");

        Label categoryLbl = new Label(t.getCategory().name() + " • " + t.getDate().toString());
        categoryLbl.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");

        metaBox.getChildren().addAll(titleLbl, categoryLbl);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String sign = (t.getType() == TransactionType.INCOME) ? "+ " : "- ";
        Label amountLbl = new Label(sign + currencySymbol + " " + t.getAmount());

        if (t.getType() == TransactionType.INCOME) {
            amountLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-font-size: 13px;");
        } else {
            amountLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        }

        row.getChildren().addAll(metaBox, spacer, amountLbl);
        return row;
    }
}