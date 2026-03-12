package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.CategoryGroup;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.enums.TransactionStatus;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.service.impl.CategoryServiceImpl;
import com.qsaj.balarama.v1.service.FinanceService;
import com.qsaj.balarama.v1.session.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.util.Callback;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;
    @FXML private Label expenseLabel;
    @FXML private Label incomeLabel;
    @FXML private BarChart<String, Number> expenseBarChart;
    @FXML private VBox recentTransactionsContainer;
    @FXML private GridPane calendarGrid;
    @FXML private Label calendarMonthLabel;
    @FXML private ComboBox<TransactionStatus> calendarFilterCombo;
    @FXML private VBox chartLegendContainer;
    @FXML private Label insightTitleLabel;
    @FXML private Label insightTextLabel;
    @FXML private ProgressBar budgetProgressBar;
    @FXML private Label budgetPercentLabel;

    private final UserSession userSession;
    private final FinanceService financeService;
    private final CategoryServiceImpl categoryService;
    private YearMonth currentYearMonth;
    private List<Transaction> allTransactions = Collections.emptyList();
    private final Popup calendarPopup = new Popup();
    private TransactionStatus selectedCalendarStatus = null;

    @Inject
    public DashboardController(UserSession userSession, FinanceService financeService, CategoryServiceImpl categoryService) {
        this.userSession = userSession;
        this.financeService = financeService;
        this.categoryService = categoryService;
        this.calendarPopup.setAutoHide(true);
    }

    @FXML
    public void initialize() {
        currentYearMonth = YearMonth.now();
        expenseBarChart.getStyleClass().add("expense-chart");
        expenseBarChart.setAnimated(false);
        CategoryAxis xAxis = (CategoryAxis) expenseBarChart.getXAxis();
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setOpacity(0);

        setupCalendarFilter();
        refresh();
    }

    private void setupCalendarFilter() {
        calendarFilterCombo.setItems(FXCollections.observableArrayList(
                null, TransactionStatus.PENDING, TransactionStatus.COMPLETED, TransactionStatus.OVERDUE
        ));

        Callback<ListView<TransactionStatus>, ListCell<TransactionStatus>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(TransactionStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All Statuses" : item.name());
            }
        };

        calendarFilterCombo.setCellFactory(cellFactory);
        calendarFilterCombo.setButtonCell(cellFactory.call(null));

        calendarFilterCombo.valueProperty().addListener((o, oldV, newV) -> {
            selectedCalendarStatus = newV;
            populateCalendar();
        });
    }

    private void refresh() {
        var user = userSession.getCurrentUser();
        String symbol = user.getCurrency() != null ? user.getCurrency().getSymbol() : "$";

        welcomeLabel.setText("Hi, " + user.getUsername() + "!");
        allTransactions = financeService.getUserTransactions(user);

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        Map<CategoryGroup, BigDecimal> expenseByGroup = new HashMap<>();

        for (Transaction t : allTransactions) {
            if (t.getType() == TransactionType.INCOME) {
                income = income.add(t.getAmount());
            } else {
                expense = expense.add(t.getAmount());
                CategoryGroup group = t.getCategoryItem().getGroup();
                expenseByGroup.merge(group, t.getAmount(), BigDecimal::add);
            }
        }

        BigDecimal balance = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;

        balanceLabel.setText(symbol + " " + balance);
        incomeLabel.setText("+ " + symbol + " " + income);
        expenseLabel.setText("- " + symbol + " " + expense);

        // Передаємо символ валюти у метод побудови графіка
        updateBarChart(expenseByGroup, symbol);
        updateRecentTransactions(symbol);
        updateInsight(balance, income, expense);
        populateCalendar();
    }

    private void updateBarChart(Map<CategoryGroup, BigDecimal> data, String symbol) {
        expenseBarChart.getData().clear();
        chartLegendContainer.getChildren().clear();

        if (data.isEmpty()) return;

        List<Map.Entry<CategoryGroup, BigDecimal>> entries = new ArrayList<>(data.entrySet());
        entries.sort(Comparator.comparing(e -> e.getKey().getName()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Expenses");

        for (Map.Entry<CategoryGroup, BigDecimal> entry : entries) {
            CategoryGroup group = entry.getKey();
            BigDecimal value = entry.getValue();

            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(group.getName(), value);
            final String color = group.getColor();

            dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    // Встановлюємо колір стовпця
                    newNode.setStyle("-fx-bar-fill: " + color + ";");

                    // --- НОВЕ: Створюємо та додаємо підказку (Tooltip) ---
                    Tooltip tooltip = new Tooltip(group.getName() + ":\n" + symbol + " " + value);
                    tooltip.setShowDelay(Duration.ZERO); // Миттєва поява при наведенні
                    tooltip.getStyleClass().add("chart-tooltip");
                    Tooltip.install(newNode, tooltip);

                    // Робимо так, щоб курсор ставав "рукою" при наведенні на стовпець
                    newNode.setCursor(javafx.scene.Cursor.HAND);
                }
            });

            series.getData().add(dataPoint);

            HBox legendItem = new HBox(8);
            legendItem.setAlignment(Pos.CENTER_LEFT);
            legendItem.getStyleClass().add("chart-legend-item");

            Region colorBox = new Region();
            colorBox.setMinSize(12, 12);
            colorBox.setMaxSize(12, 12);
            colorBox.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");

            Label catLabel = new Label(group.getName());
            catLabel.getStyleClass().add("chart-legend-label");

            legendItem.getChildren().addAll(colorBox, catLabel);
            chartLegendContainer.getChildren().add(legendItem);
        }

        expenseBarChart.getData().add(series);
    }

    private void updateRecentTransactions(String symbol) {
        recentTransactionsContainer.getChildren().clear();

        List<Transaction> recent = allTransactions.stream()
                .sorted(Comparator.comparing(Transaction::getId).reversed())
                .limit(5)
                .toList();

        if (recent.isEmpty()) {
            Label placeholder = new Label("No recent activity.");
            placeholder.getStyleClass().add("recent-tx-loading");
            recentTransactionsContainer.getChildren().add(placeholder);
            return;
        }

        for (Transaction t : recent) {
            recentTransactionsContainer.getChildren().add(createTransactionRow(t, symbol));
        }
    }

    private HBox createTransactionRow(Transaction t, String symbol) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(15);
        row.getStyleClass().add("recent-tx-row");

        VBox metaBox = new VBox(3);
        Label titleLbl = new Label(t.getTitle());
        titleLbl.getStyleClass().add("recent-tx-title");

        String catInfo = t.getCategoryItem().getGroup().getName() + " → " + t.getCategoryItem().getName() + " • " + t.getDate().toString();
        Label catLbl = new Label(catInfo);
        catLbl.getStyleClass().add("recent-tx-meta");

        metaBox.getChildren().addAll(titleLbl, catLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String sign = t.getType() == TransactionType.INCOME ? "+ " : "- ";
        Label amtLbl = new Label(sign + symbol + " " + t.getAmount());

        if (t.getType() == TransactionType.INCOME) {
            amtLbl.getStyleClass().add("recent-tx-income");
        } else {
            amtLbl.getStyleClass().add("recent-tx-expense");
        }

        row.getChildren().addAll(metaBox, spacer, amtLbl);
        return row;
    }

    private void updateInsight(BigDecimal balance, BigDecimal income, BigDecimal expense) {
        double progress = income.signum() > 0
                ? expense.divide(income, 2, RoundingMode.HALF_UP).doubleValue()
                : 1.0;

        budgetProgressBar.setProgress(Math.min(progress, 1));
        budgetPercentLabel.setText((int) (progress * 100) + "%");

        budgetProgressBar.getStyleClass().removeAll("budget-bar-ok", "budget-bar-danger");
        insightTitleLabel.getStyleClass().removeAll("insight-ok", "insight-warning", "insight-danger");

        if (balance.signum() < 0) {
            insightTitleLabel.setText("Negative Balance");
            insightTextLabel.setText("Reduce spending immediately.");
            insightTitleLabel.getStyleClass().add("insight-danger");
            budgetProgressBar.getStyleClass().add("budget-bar-danger");
        } else if (progress > 0.9) {
            insightTitleLabel.setText("Budget Alert");
            insightTextLabel.setText("High spending detected.");
            insightTitleLabel.getStyleClass().add("insight-warning");
            budgetProgressBar.getStyleClass().add("budget-bar-danger");
        } else {
            insightTitleLabel.setText("Healthy Finances");
            insightTextLabel.setText("Keep up the good work.");
            insightTitleLabel.getStyleClass().add("insight-ok");
            budgetProgressBar.getStyleClass().add("budget-bar-ok");
        }
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
        calendarMonthLabel.setText(currentYearMonth.getMonth().toString() + " " + currentYearMonth.getYear());

        LocalDate first = currentYearMonth.atDay(1);
        int dayOfWeek = first.getDayOfWeek().getValue();
        int daysInMonth = currentYearMonth.lengthOfMonth();

        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            Label l = new Label(days[i]);
            l.getStyleClass().add("calendar-day-header");
            calendarGrid.add(l, i, 0);
        }

        int row = 1;
        int col = dayOfWeek - 1;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);

            List<Transaction> txs = allTransactions.stream()
                    .filter(t -> {
                        LocalDate targetDate = (t.getDueDate() != null) ? t.getDueDate() : t.getDate();
                        return targetDate.equals(date);
                    })
                    .filter(t -> selectedCalendarStatus == null || t.getStatus() == selectedCalendarStatus)
                    .collect(Collectors.toList());

            StackPane cell = new StackPane();
            cell.setPrefSize(35, 35);
            cell.getStyleClass().add("calendar-cell");

            if (date.equals(LocalDate.now())) cell.getStyleClass().add("calendar-cell-today");

            boolean hasPending = txs.stream().anyMatch(t -> t.getStatus() == TransactionStatus.PENDING);
            boolean hasCompleted = txs.stream().anyMatch(t -> t.getStatus() == TransactionStatus.COMPLETED);
            boolean hasOverdue = txs.stream().anyMatch(t -> t.getStatus() == TransactionStatus.OVERDUE);

            if (hasPending) cell.getStyleClass().add("calendar-cell-pending");
            else if (hasCompleted) cell.getStyleClass().add("calendar-cell-completed");
            else if (hasOverdue) cell.getStyleClass().add("calendar-cell-overdue");

            Label dayLbl = new Label(String.valueOf(day));
            dayLbl.getStyleClass().add("calendar-text-default");

            if (hasPending) dayLbl.getStyleClass().add("calendar-text-pending");
            else if (hasCompleted) dayLbl.getStyleClass().add("calendar-text-completed");
            else if (hasOverdue) dayLbl.getStyleClass().add("calendar-text-overdue");

            cell.getChildren().add(dayLbl);

            if (!txs.isEmpty()) {
                cell.setOnMouseEntered(e -> showPopup(cell, txs, e.getScreenX(), e.getScreenY()));
                cell.setOnMouseExited(e -> calendarPopup.hide());
            }
            calendarGrid.add(cell, col, row);
            if (++col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private void showPopup(StackPane node, List<Transaction> txs, double x, double y) {
        VBox box = new VBox(5);
        box.getStyleClass().add("calendar-popup-box");
        box.getStyleClass().add("root");
        box.getStylesheets().addAll(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm(),
                Objects.requireNonNull(getClass().getResource("/css/dashboard.css")).toExternalForm()
        );

        Label head = new Label(txs.get(0).getDate().toString());
        head.getStyleClass().add("calendar-popup-header");
        box.getChildren().add(head);

        for (Transaction t : txs) {
            String styleClass;
            String icon;

            switch (t.getStatus()) {
                case COMPLETED -> {
                    icon = "✓";
                    styleClass = "popup-status-completed";
                }
                case PENDING -> {
                    icon = "⏳";
                    styleClass = "popup-status-pending";
                }
                default -> {
                    icon = "❗";
                    styleClass = "popup-status-overdue";
                }
            }

            String cat = t.getCategoryItem().getGroup().getName() + " → " + t.getCategoryItem().getName();
            Label l = new Label(icon + " " + t.getTitle() + " (" + cat + "): " + t.getAmount());
            l.getStyleClass().addAll("calendar-popup-text", styleClass);
            box.getChildren().add(l);
        }

        calendarPopup.getContent().clear();
        calendarPopup.getContent().add(box);
        calendarPopup.show(node.getScene().getWindow(), x + 10, y + 10);
    }
}