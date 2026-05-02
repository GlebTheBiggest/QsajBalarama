package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.BudgetPeriod;
import com.qsaj.balarama.v1.model.MLInsight;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.AppMode;
import com.qsaj.balarama.v1.model.enums.Priority;
import com.qsaj.balarama.v1.model.enums.TransactionStatus;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.service.BudgetService;
import com.qsaj.balarama.v1.service.FinanceService;
import com.qsaj.balarama.v1.service.MLService;
import com.qsaj.balarama.v1.service.NavigationService;
import com.qsaj.balarama.v1.session.UserSession;
import com.qsaj.balarama.v1.util.SmartDateFormatter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsController {

    @FXML private ComboBox<String> periodComboBox;
    @FXML private DatePicker customStartDate;
    @FXML private DatePicker customEndDate;
    @FXML private BarChart<String, Number> categoryChart;
    @FXML private LineChart<String, Number> trendChart;
    @FXML private BarChart<String, Number> priorityChart;
    @FXML private PieChart statusPieChart;
    @FXML private VBox insightsContainer;
    @FXML private Label totalSpentLabel;
    @FXML private Label averageMonthlyLabel;
    @FXML private Label topCategoryLabel;

    private final UserSession userSession;
    private final FinanceService financeService;
    private final MLService mlService;
    private final BudgetService budgetService;
    private final NavigationService navigationService;
    private List<Transaction> allTransactions;
    private LocalDate currentStartDate;
    private LocalDate currentEndDate;
    private BudgetPeriod selectedPeriod;
    private ObservableList<String> periodItems = FXCollections.observableArrayList();

    // Поля для збереження шматків торту назавжди
    private PieChart.Data completedSlice;
    private PieChart.Data pendingSlice;
    private PieChart.Data overdueSlice;
    private PieChart.Data noDataSlice;
    private boolean pieInitialized = false;
    private final Map<PieChart.Data, Tooltip> tooltipMap = new HashMap<>();
    private final Map<PieChart.Data, String> tooltipTextMap = new HashMap<>();

    @Inject
    public AnalyticsController(UserSession userSession, FinanceService financeService,
                               MLService mlService, BudgetService budgetService,
                               NavigationService navigationService) {
        this.userSession = userSession;
        this.financeService = financeService;
        this.mlService = mlService;
        this.budgetService = budgetService;
        this.navigationService = navigationService;
    }

    @FXML
    public void initialize() {
        setupPeriodComboBox();
        SmartDateFormatter.apply(customStartDate);
        SmartDateFormatter.apply(customEndDate);

        customStartDate.valueProperty().addListener((obs, oldVal, newVal) -> applyCustomFilter());
        customEndDate.valueProperty().addListener((obs, oldVal, newVal) -> applyCustomFilter());

        loadPeriodsIntoComboBox();

        User user = userSession.getCurrentUser();
        String initialPeriodLabel = "All time";

        if (user.getAppMode() == AppMode.PERIODIC) {
            BudgetPeriod active = budgetService.getActivePeriod(user);
            if (active != null) {
                initialPeriodLabel = formatPeriodLabel(active);
            }
        }

        periodComboBox.setValue(initialPeriodLabel);

        loadData();
        updateChartsAndInsights();
    }

    private void setupPeriodComboBox() {
        periodComboBox.setItems(periodItems);
        periodComboBox.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
            }
        });
        periodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            if ("All time".equals(newVal)) {
                selectedPeriod = null;
                customStartDate.setValue(null);
                customEndDate.setValue(null);
                customStartDate.setDisable(true);
                customEndDate.setDisable(true);
                currentStartDate = null;
                currentEndDate = null;
                resetDateCellFactories(); // Знімаємо обмеження з календарів
                loadData();
                updateChartsAndInsights();
            } else if ("Custom dates".equals(newVal)) {
                selectedPeriod = null;
                customStartDate.setDisable(false);
                customEndDate.setDisable(false);
                customStartDate.setValue(currentStartDate);
                customEndDate.setValue(currentEndDate);
                resetDateCellFactories(); // Знімаємо обмеження з календарів
            } else {
                for (BudgetPeriod p : budgetService.getAllPeriods(userSession.getCurrentUser())) {
                    if (formatPeriodLabel(p).equals(newVal)) {
                        selectedPeriod = p;
                        currentStartDate = p.getStartDate();
                        currentEndDate = p.getEndDate();

                        // Фізично блокуємо дати поза межами обраного періоду
                        restrictDatePickersToPeriod(p.getStartDate(), p.getEndDate());

                        customStartDate.setValue(currentStartDate);
                        customEndDate.setValue(currentEndDate);
                        customStartDate.setDisable(false);
                        customEndDate.setDisable(false);
                        loadData();
                        updateChartsAndInsights();
                        break;
                    }
                }
            }
        });
    }

    // Блокує дати в календарі, які не входять у вибраний період
    private void restrictDatePickersToPeriod(LocalDate startBound, LocalDate endBound) {
        Callback<DatePicker, DateCell> dayCellFactory = dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isBefore(startBound) || item.isAfter(endBound)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;"); // Світло-червоний фон для заблокованих дат
                }
            }
        };
        customStartDate.setDayCellFactory(dayCellFactory);
        customEndDate.setDayCellFactory(dayCellFactory);
    }

    // Знімає всі обмеження з календарів
    private void resetDateCellFactories() {
        customStartDate.setDayCellFactory(null);
        customEndDate.setDayCellFactory(null);
    }

    private String formatPeriodLabel(BudgetPeriod period) {
        String label = period.getStartDate() + " – " + period.getEndDate();
        if (period.isActive()) {
            label += " (Active)";
        }
        return label;
    }

    private void loadPeriodsIntoComboBox() {
        periodItems.clear();
        periodItems.add("All time");
        User user = userSession.getCurrentUser();
        List<BudgetPeriod> periods = budgetService.getAllPeriods(user);
        periods.sort((p1, p2) -> p2.getStartDate().compareTo(p1.getStartDate()));
        Set<String> seen = new HashSet<>();
        for (BudgetPeriod p : periods) {
            String label = formatPeriodLabel(p);
            if (!seen.contains(label)) {
                seen.add(label);
                periodItems.add(label);
            }
        }
        periodItems.add("Custom dates");
    }

    private void applyCustomFilter() {
        if (!"Custom dates".equals(periodComboBox.getValue()) && selectedPeriod == null) return;

        LocalDate start = SmartDateFormatter.getDate(customStartDate);
        LocalDate end = SmartDateFormatter.getDate(customEndDate);
        if (start == null || end == null) return;

        if (start.isAfter(end)) {
            navigationService.showAlert("Invalid Range", "Start date cannot be after end date.");
            customStartDate.setValue(currentStartDate);
            customEndDate.setValue(currentEndDate);
            return;
        }

        // Додаткова перевірка, навіть попри обмеження в календарі (на випадок ручного вводу тексту)
        if (selectedPeriod != null) {
            if (start.isBefore(selectedPeriod.getStartDate()) || end.isAfter(selectedPeriod.getEndDate())) {
                navigationService.showAlert("Invalid Range",
                        "Dates must be within the selected period: " +
                                selectedPeriod.getStartDate() + " – " + selectedPeriod.getEndDate());
                customStartDate.setValue(currentStartDate);
                customEndDate.setValue(currentEndDate);
                return;
            }
        }

        currentStartDate = start;
        currentEndDate = end;
        loadData();
        updateChartsAndInsights();
    }

    private void loadData() {
        User user = userSession.getCurrentUser();
        if (currentStartDate == null || currentEndDate == null) {
            allTransactions = financeService.getUserTransactions(user).stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .collect(Collectors.toList());
        } else {
            allTransactions = financeService.getUserTransactions(user).stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .filter(t -> !t.getDate().isBefore(currentStartDate) && !t.getDate().isAfter(currentEndDate))
                    .collect(Collectors.toList());
        }
    }

    private void updateChartsAndInsights() {
        updateSummaryStats();
        updateCategoryChart();
        updateTrendChart();
        updatePriorityChart();
        updateStatusPieChart();
        updateInsights();
    }

    private void updateSummaryStats() {
        BigDecimal total = allTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String symbol = userSession.getCurrentUser().getCurrency().getSymbol();
        totalSpentLabel.setText(symbol + " " + total);

        if (allTransactions.isEmpty()) {
            averageMonthlyLabel.setText(symbol + " 0");
        } else if (currentStartDate == null || currentEndDate == null) {
            Optional<LocalDate> minDate = allTransactions.stream().map(Transaction::getDate).min(LocalDate::compareTo);
            Optional<LocalDate> maxDate = allTransactions.stream().map(Transaction::getDate).max(LocalDate::compareTo);
            if (minDate.isPresent() && maxDate.isPresent()) {
                long months = ChronoUnit.MONTHS.between(YearMonth.from(minDate.get()), YearMonth.from(maxDate.get())) + 1;
                if (months > 0) {
                    BigDecimal avg = total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
                    averageMonthlyLabel.setText(symbol + " " + avg);
                } else {
                    averageMonthlyLabel.setText(symbol + " " + total);
                }
            } else {
                averageMonthlyLabel.setText(symbol + " 0");
            }
        } else {
            long months = ChronoUnit.MONTHS.between(YearMonth.from(currentStartDate), YearMonth.from(currentEndDate)) + 1;
            if (months > 0 && months <= 120) {
                BigDecimal avg = total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
                averageMonthlyLabel.setText(symbol + " " + avg);
            } else {
                averageMonthlyLabel.setText(symbol + " 0");
            }
        }

        if (allTransactions.isEmpty()) {
            topCategoryLabel.setText("—");
        } else {
            Map<String, BigDecimal> catSum = allTransactions.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getSnapshotGroupName() != null ? t.getSnapshotGroupName() : "Unknown",
                            Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                    ));
            Optional<Map.Entry<String, BigDecimal>> top = catSum.entrySet().stream()
                    .max(Map.Entry.comparingByValue());
            if (top.isPresent() && top.get().getValue().compareTo(BigDecimal.ZERO) > 0) {
                topCategoryLabel.setText(top.get().getKey() + " (" + symbol + " " + top.get().getValue() + ")");
            } else {
                topCategoryLabel.setText("—");
            }
        }
    }

    private void updateCategoryChart() {
        categoryChart.getData().clear();

        Map<String, String> categoryColors = new HashMap<>();
        for (Transaction t : allTransactions) {
            String cat = t.getSnapshotGroupName();
            String color = t.getSnapshotColor();
            if (cat != null && color != null && !categoryColors.containsKey(cat)) {
                categoryColors.put(cat, color);
            }
        }

        Map<String, BigDecimal> catSum = allTransactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getSnapshotGroupName() != null ? t.getSnapshotGroupName() : "Unknown",
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Expenses");

        for (Map.Entry<String, BigDecimal> entry : catSum.entrySet()) {
            String category = entry.getKey();
            BigDecimal amount = entry.getValue();
            XYChart.Data<String, Number> data = new XYChart.Data<>(category, amount);

            String color = categoryColors.getOrDefault(category, "#3498db");
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-bar-fill: " + color + ";");
                    Tooltip tooltip = new Tooltip(category + ": " + amount);
                    tooltip.setShowDelay(Duration.ZERO);
                    Tooltip.install(newNode, tooltip);
                }
            });
            series.getData().add(data);
        }

        categoryChart.getData().add(series);
        categoryChart.setAnimated(false);
    }

    private void updateTrendChart() {
        trendChart.getData().clear();
        Map<YearMonth, BigDecimal> monthlyTotal = allTransactions.stream()
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getDate()),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Monthly Spending");
        List<YearMonth> sortedMonths = new ArrayList<>(monthlyTotal.keySet());
        sortedMonths.sort(YearMonth::compareTo);
        for (YearMonth ym : sortedMonths) {
            series.getData().add(new XYChart.Data<>(ym.format(DateTimeFormatter.ofPattern("MMM yyyy")), monthlyTotal.get(ym)));
        }
        trendChart.getData().add(series);
        trendChart.setAnimated(false);
    }

    private void updatePriorityChart() {
        priorityChart.getData().clear();

        Map<Priority, BigDecimal> prioritySum = allTransactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getPriority,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("By Priority");

        String[] colors = {"#e74c3c", "#f39c12", "#2ecc71"};
        Priority[] priorities = {Priority.ESSENTIAL, Priority.STANDARD, Priority.OPTIONAL};

        for (int i = 0; i < priorities.length; i++) {
            Priority p = priorities[i];
            BigDecimal amount = prioritySum.getOrDefault(p, BigDecimal.ZERO);
            XYChart.Data<String, Number> data = new XYChart.Data<>(p.name(), amount);
            String color = colors[i];
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-bar-fill: " + color + ";");
                    Tooltip tooltip = new Tooltip(p.name() + ": " + amount);
                    tooltip.setShowDelay(Duration.ZERO);
                    Tooltip.install(newNode, tooltip);
                }
            });
            series.getData().add(data);
        }

        priorityChart.getData().add(series);
        priorityChart.setAnimated(false);
    }

    private void updateStatusPieChart() {
        Map<TransactionStatus, Long> statusCount = allTransactions.stream()
                .collect(Collectors.groupingBy(Transaction::getStatus, Collectors.counting()));

        long completed = statusCount.getOrDefault(TransactionStatus.COMPLETED, 0L);
        long pending = statusCount.getOrDefault(TransactionStatus.PENDING, 0L);
        long overdue = statusCount.getOrDefault(TransactionStatus.OVERDUE, 0L);
        long total = completed + pending + overdue;

        if (!pieInitialized) {
            completedSlice = new PieChart.Data(TransactionStatus.COMPLETED.name(), 0);
            pendingSlice = new PieChart.Data(TransactionStatus.PENDING.name(), 0);
            overdueSlice = new PieChart.Data(TransactionStatus.OVERDUE.name(), 0);
            noDataSlice = new PieChart.Data("No data", 0);

            statusPieChart.getData().addAll(completedSlice, pendingSlice, overdueSlice, noDataSlice);

            statusPieChart.setLabelsVisible(false);
            statusPieChart.setClockwise(true);
            statusPieChart.setLabelLineLength(10);

            setupSlice(completedSlice, "#2ecc71");
            setupSlice(pendingSlice, "#f39c12");
            setupSlice(overdueSlice, "#e74c3c");
            setupSlice(noDataSlice, "#95a5a6");

            pieInitialized = true;
        }

        if (total == 0) {
            completedSlice.setPieValue(0);
            pendingSlice.setPieValue(0);
            overdueSlice.setPieValue(0);
            noDataSlice.setPieValue(1);

            updateTooltipText(noDataSlice, "No transactions");
        } else {
            completedSlice.setPieValue(completed);
            pendingSlice.setPieValue(pending);
            overdueSlice.setPieValue(overdue);
            noDataSlice.setPieValue(0);

            updateTooltipText(completedSlice, TransactionStatus.COMPLETED.name() + ": " + completed + " transactions");
            updateTooltipText(pendingSlice, TransactionStatus.PENDING.name() + ": " + pending + " transactions");
            updateTooltipText(overdueSlice, TransactionStatus.OVERDUE.name() + ": " + overdue + " transactions");
        }

        Platform.runLater(() -> {
            if (completedSlice.getNode() != null) completedSlice.getNode().setVisible(completed > 0);
            if (pendingSlice.getNode() != null) pendingSlice.getNode().setVisible(pending > 0);
            if (overdueSlice.getNode() != null) overdueSlice.getNode().setVisible(overdue > 0);
            if (noDataSlice.getNode() != null) noDataSlice.getNode().setVisible(total == 0);

            for (Node node : statusPieChart.lookupAll(".chart-legend-item")) {
                if (node instanceof Label label) {
                    String text = label.getText();
                    if ("No data".equals(text)) {
                        label.setManaged(false);
                        label.setVisible(false);
                    } else {
                        label.setManaged(true);
                        label.setVisible(true);
                        if (label.getGraphic() != null) {
                            if (TransactionStatus.COMPLETED.name().equals(text)) {
                                label.getGraphic().setStyle("-fx-background-color: #2ecc71;");
                            } else if (TransactionStatus.PENDING.name().equals(text)) {
                                label.getGraphic().setStyle("-fx-background-color: #f39c12;");
                            } else if (TransactionStatus.OVERDUE.name().equals(text)) {
                                label.getGraphic().setStyle("-fx-background-color: #e74c3c;");
                            }
                        }
                    }
                }
            }
        });
    }

    private void setupSlice(PieChart.Data slice, String color) {
        if (slice.getNode() != null) {
            applySliceStyleAndTooltip(slice, slice.getNode(), color);
        }
        slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                applySliceStyleAndTooltip(slice, newNode, color);
            }
        });
    }

    private void applySliceStyleAndTooltip(PieChart.Data slice, Node node, String color) {
        node.setStyle("-fx-pie-color: " + color + ";");
        if (!tooltipMap.containsKey(slice)) {
            Tooltip tooltip = new Tooltip();
            tooltip.setShowDelay(Duration.ZERO);
            Tooltip.install(node, tooltip);
            tooltipMap.put(slice, tooltip);
        }
        updateTooltipText(slice, tooltipTextMap.getOrDefault(slice, ""));
    }

    private void updateTooltipText(PieChart.Data slice, String text) {
        tooltipTextMap.put(slice, text);
        Tooltip tooltip = tooltipMap.get(slice);
        if (tooltip != null) {
            tooltip.setText(text);
        }
    }

    private void updateInsights() {
        insightsContainer.getChildren().clear();

        // Викликаємо сервіс з поточними датами (якщо All time - вони будуть null)
        List<MLInsight> insights = mlService.generateInsights(
                userSession.getCurrentUser(), currentStartDate, currentEndDate);

        for (MLInsight insight : insights) {
            VBox card = new VBox(8);
            card.getStyleClass().add("insight-card");

            Label titleLabel = new Label(insight.getTitle());
            titleLabel.getStyleClass().add("insight-title");

            Label descLabel = new Label(insight.getDescription());
            descLabel.setWrapText(true);
            descLabel.getStyleClass().add("insight-description");

            Label suggLabel = new Label("💡 " + insight.getSuggestion());
            suggLabel.setWrapText(true);
            suggLabel.getStyleClass().add("insight-suggestion");

            Label confLabel = new Label(String.format("Confidence: %.0f%%", insight.getConfidence() * 100));
            confLabel.getStyleClass().add("insight-confidence");

            card.getChildren().addAll(titleLabel, descLabel, suggLabel, confLabel);
            insightsContainer.getChildren().add(card);
        }

        if (insights.isEmpty()) {
            Label empty = new Label("No insights available for the selected period.");
            empty.getStyleClass().add("insight-empty");
            insightsContainer.getChildren().add(empty);
        }
    }
}