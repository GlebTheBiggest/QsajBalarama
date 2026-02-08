package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.dto.DashboardStats;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.Currency;
import com.qsaj.balarama.v1.model.enums.TransactionStatus;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.service.FinanceService;
import com.qsaj.balarama.v1.session.UserSession;
import com.qsaj.balarama.v1.util.CalendarCellFactory;
import com.qsaj.balarama.v1.util.DashboardCalculator;
import com.qsaj.balarama.v1.util.Insight;
import com.qsaj.balarama.v1.util.InsightCalculator;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.stage.Popup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;
    @FXML private Label expenseLabel;
    @FXML private Label incomeLabel;
    @FXML private PieChart expensePieChart;
    @FXML private VBox recentTransactionsContainer;
    @FXML private GridPane calendarGrid;
    @FXML private Label calendarMonthLabel;
    @FXML private Label insightTitleLabel;
    @FXML private Label insightTextLabel;
    @FXML private ProgressBar budgetProgressBar;
    @FXML private Label budgetPercentLabel;

    private final UserSession userSession;
    private final FinanceService financeService;
    private YearMonth currentYearMonth;
    private List<Transaction> transactions;
    private Map<LocalDate, List<Transaction>> transactionsByDate;
    private final Popup calendarPopup = new Popup();

    @Inject
    public DashboardController(UserSession userSession, FinanceService financeService) {
        this.userSession = userSession;
        this.financeService = financeService;
    }

    @FXML
    public void initialize() {
        currentYearMonth = YearMonth.now();
        refresh();
    }

    private void refresh() {
        User user = userSession.getCurrentUser();
        Currency currency = user.getCurrency() != null ? user.getCurrency() : Currency.USD;

        welcomeLabel.setText("Hi, " + user.getUsername() + "!");
        welcomeLabel.getStyleClass().add("welcome-text");

        transactions = financeService.getUserTransactions(user);
        transactionsByDate = mapTransactionsByDate(transactions);

        DashboardStats stats = DashboardCalculator.calculate(transactions);

        BigDecimal balance = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;

        balanceLabel.setText(formatMoney(balance, currency));
        incomeLabel.setText("+ " + formatMoney(stats.getIncome(), currency));
        expenseLabel.setText("- " + formatMoney(stats.getExpense(), currency));

        updatePieChart(stats);
        updateRecentTransactions(currency);
        updateInsight(balance, stats.getIncome(), stats.getExpense());
        populateCalendar();
    }

    private String formatMoney(BigDecimal value, Currency currency) {
        return String.format("%s %s", currency.getSymbol(), value.setScale(2, RoundingMode.HALF_UP));
    }

    private void updatePieChart(DashboardStats stats) {
        expensePieChart.getData().clear();

        if (stats.getExpenseByCategory().isEmpty()) {
            expensePieChart.setTitle("No Expense Data Yet");
            expensePieChart.setLegendVisible(false);
            return;
        }

        expensePieChart.setTitle("");
        expensePieChart.setLegendVisible(false);

        stats.getExpenseByCategory().forEach((category, amount) ->
                expensePieChart.getData().add(new PieChart.Data(category, amount.doubleValue()))
        );
    }

    private void updateRecentTransactions(Currency currency) {
        recentTransactionsContainer.getChildren().clear();

        List<Transaction> recent = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .limit(5)
                .toList();

        if (recent.isEmpty()) {
            recentTransactionsContainer.getChildren().add(new Label("No recent transactions."));
            return;
        }

        recent.forEach(t -> recentTransactionsContainer.getChildren().add(createTransactionRow(t, currency)));
    }

    private HBox createTransactionRow(Transaction t, Currency currency) {
        Label title = new Label(t.getTitle());
        Label meta = new Label(t.getCategory() + " • " + t.getDate());

        VBox metaBox = new VBox(3, title, meta);

        String amountText = (t.getType() == TransactionType.INCOME ? "+ " : "- ") + formatMoney(t.getAmount(), currency);
        Label amount = new Label(amountText);

        if (t.getType() == TransactionType.INCOME) {
            amount.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(15, metaBox, spacer, amount);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("transaction-row");

        return row;
    }

    private void updateInsight(BigDecimal balance, BigDecimal income, BigDecimal expense) {
        Insight insight = InsightCalculator.calculate(balance, income, expense);

        budgetProgressBar.setProgress(Math.min(insight.progress(), 1));
        budgetPercentLabel.setText((int) (insight.progress() * 100) + "%");

        insightTitleLabel.setText(insight.title());
        insightTextLabel.setText(insight.text());
    }

    @FXML
    private void prevMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        populateCalendar();
    }

    @FXML
    private void nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        populateCalendar();
    }

    private void populateCalendar() {
        calendarGrid.getChildren().clear();
        calendarMonthLabel.setText(String.format("%s %d", currentYearMonth.getMonth(), currentYearMonth.getYear()));

        LocalDate firstDay = currentYearMonth.atDay(1);
        int col = firstDay.getDayOfWeek().getValue() - 1;
        int row = 1;

        for (int day = 1; day <= currentYearMonth.lengthOfMonth(); day++) {
            LocalDate date = currentYearMonth.atDay(day);
            List<Transaction> dayTransactions = transactionsByDate.getOrDefault(date, List.of());

            StackPane cell = CalendarCellFactory.create(day, date, dayTransactions, calendarPopup);
            calendarGrid.add(cell, col, row);

            if (++col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private Map<LocalDate, List<Transaction>> mapTransactionsByDate(List<Transaction> transactions) {
        return transactions.stream()
                .flatMap(t -> {
                    if (t.getDueDate() != null && !t.getDueDate().equals(t.getDate())) {
                        return Stream.of(
                                Map.entry(t.getDate(), t),
                                Map.entry(t.getDueDate(), t)
                        );
                    }
                    return Stream.of(Map.entry(t.getDate(), t));
                })
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }
}