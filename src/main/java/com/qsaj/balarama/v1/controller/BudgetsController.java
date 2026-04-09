package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.BudgetPeriod;
import com.qsaj.balarama.v1.model.CategoryBudget;
import com.qsaj.balarama.v1.model.CategoryGroup;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.AppMode;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.repository.UserRepository;
import com.qsaj.balarama.v1.service.BudgetService;
import com.qsaj.balarama.v1.service.FinanceService;
import com.qsaj.balarama.v1.service.NavigationService;
import com.qsaj.balarama.v1.service.SavingsService;
import com.qsaj.balarama.v1.service.impl.CategoryServiceImpl;
import com.qsaj.balarama.v1.session.UserSession;
import com.qsaj.balarama.v1.util.SmartDateFormatter;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BudgetsController {

    @FXML private Label modeLabel;
    @FXML private Button switchModeBtn;
    @FXML private Label periodInfoLabel;
    @FXML private Label datesLabel;
    @FXML private HBox newPeriodBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button startPeriodBtn;
    @FXML private Button closePeriodBtn;
    @FXML private Button saveBudgetsBtn;

    @FXML private Label totalBudgetedLabel;
    @FXML private Label totalSpentLabel;
    @FXML private Label totalRemainingLabel;

    @FXML private VBox budgetsContainer;

    private final UserSession userSession;
    private final BudgetService budgetService;
    private final CategoryServiceImpl categoryService;
    private final UserRepository userRepo;
    private final NavigationService nav;
    private final SavingsService savingsService;
    private final FinanceService financeService;

    private BudgetPeriod currentPeriod;
    private final Map<CategoryGroup, BudgetRowUI> rowUIMap = new HashMap<>();

    private static class BudgetRowUI {
        CheckBox enableCheckBox;
        TextField allocInput;
        Label spentRemainingLabel;
        ProgressBar progressBar;
    }

    @Inject
    public BudgetsController(UserSession userSession, BudgetService budgetService,
                             CategoryServiceImpl categoryService, UserRepository userRepo,
                             NavigationService nav, SavingsService savingsService,
                             FinanceService financeService) {
        this.userSession = userSession;
        this.budgetService = budgetService;
        this.categoryService = categoryService;
        this.userRepo = userRepo;
        this.nav = nav;
        this.savingsService = savingsService;
        this.financeService = financeService;
    }

    @FXML
    public void initialize() {
        SmartDateFormatter.apply(startDatePicker);
        SmartDateFormatter.apply(endDatePicker);
        startDatePicker.setValue(YearMonth.now().atDay(1));
        endDatePicker.setValue(YearMonth.now().atEndOfMonth());

        refresh();
    }

    private void refresh() {
        User user = userSession.getCurrentUser();

        modeLabel.setText("Mode: " + (user.getAppMode() == AppMode.PERIODIC ? "Periodic (Envelopes)" : "Continuous"));
        switchModeBtn.setText(user.getAppMode() == AppMode.PERIODIC ?
                "Switch to Continuous" : "Enable Periodic Budgeting");

        currentPeriod = budgetService.getActivePeriod(user);
        budgetsContainer.getChildren().clear();
        rowUIMap.clear();
        updateSummary(null);

        if (user.getAppMode() == AppMode.CONTINUOUS) {
            periodInfoLabel.setText("Periodic budgeting is disabled.");
            datesLabel.setVisible(false);
            newPeriodBox.setVisible(false);
            newPeriodBox.setManaged(false);
            closePeriodBtn.setVisible(false);
            closePeriodBtn.setManaged(false);
            saveBudgetsBtn.setDisable(true);

            Label placeholder = new Label("Switch to Periodic mode to manage envelopes.");
            placeholder.getStyleClass().add("input-label");
            budgetsContainer.getChildren().add(placeholder);
            return;
        }

        if (currentPeriod == null) {
            periodInfoLabel.setText("No active budget period.");
            datesLabel.setVisible(false);
            newPeriodBox.setVisible(true);
            newPeriodBox.setManaged(true);
            closePeriodBtn.setVisible(false);
            closePeriodBtn.setManaged(false);
            saveBudgetsBtn.setDisable(true);

            Label placeholder = new Label("Start a new period to allocate budgets.");
            placeholder.getStyleClass().add("input-label");
            budgetsContainer.getChildren().add(placeholder);
        } else {
            // Видаляємо всі бюджетні записи з нульовим лімітом (захист від старих даних)
            List<CategoryBudget> budgets = budgetService.getBudgetsForPeriod(currentPeriod);
            for (CategoryBudget b : budgets) {
                if (b.getAllocatedAmount().compareTo(BigDecimal.ZERO) == 0) {
                    budgetService.removeCategoryBudget(currentPeriod, b.getCategoryGroup());
                }
            }

            periodInfoLabel.setText("Active Period");
            datesLabel.setText(currentPeriod.getStartDate() + "  –  " + currentPeriod.getEndDate());
            datesLabel.setVisible(true);
            newPeriodBox.setVisible(false);
            newPeriodBox.setManaged(false);
            closePeriodBtn.setVisible(true);
            closePeriodBtn.setManaged(true);
            saveBudgetsBtn.setDisable(false);

            buildBudgetRows();
            updateSummary(currentPeriod);
            checkPeriodExpiration();
        }
    }

    private void checkPeriodExpiration() {
        if (currentPeriod == null) return;
        if (currentPeriod.getEndDate().isBefore(LocalDate.now())) {
            User user = userSession.getCurrentUser();
            BigDecimal totalUnspent = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;
            String symbol = user.getCurrency().getSymbol();

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Period Ended");
            alert.setHeaderText("The current budget period has ended.");
            alert.setContentText("Unspent money: " + symbol + " " + totalUnspent +
                    "\n\nWhat would you like to do?\n\n" +
                    "Close Period: Unspent money will be added to your unallocated savings.\n" +
                    "Extend Period: Set a new end date.");

            ButtonType closeBtn = new ButtonType("Close Period");
            ButtonType extendBtn = new ButtonType("Extend Period");
            alert.getButtonTypes().setAll(closeBtn, extendBtn);

            alert.showAndWait().ifPresent(btn -> {
                if (btn == closeBtn) {
                    handleClosePeriod();
                } else if (btn == extendBtn) {
                    showExtendPeriodDialog();
                }
            });
        }
    }

    private void showExtendPeriodDialog() {
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Extend Period");
        dialog.setHeaderText("Select new end date for the current budget period.");

        DatePicker datePicker = new DatePicker();
        datePicker.setValue(currentPeriod.getEndDate());
        SmartDateFormatter.apply(datePicker);
        datePicker.setPromptText("DD.MM.YYYY");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
        errorLabel.setVisible(false);

        VBox content = new VBox(10);
        content.getChildren().addAll(datePicker, errorLabel);
        dialog.getDialogPane().setContent(content);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
        okButton.setDisable(true);

        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                errorLabel.setText("Please select a date.");
                errorLabel.setVisible(true);
                okButton.setDisable(true);
            } else if (!newVal.isAfter(currentPeriod.getStartDate())) {
                errorLabel.setText("End date must be after " + currentPeriod.getStartDate());
                errorLabel.setVisible(true);
                okButton.setDisable(true);
            } else {
                errorLabel.setVisible(false);
                okButton.setDisable(false);
            }
        });

        datePicker.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            LocalDate parsed = SmartDateFormatter.getDate(datePicker);
            if (parsed == null) {
                errorLabel.setText("Invalid date format. Use DD.MM.YYYY");
                errorLabel.setVisible(true);
                okButton.setDisable(true);
            } else if (!parsed.isAfter(currentPeriod.getStartDate())) {
                errorLabel.setText("End date must be after " + currentPeriod.getStartDate());
                errorLabel.setVisible(true);
                okButton.setDisable(true);
            } else {
                errorLabel.setVisible(false);
                okButton.setDisable(false);
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == okButtonType) {
                return SmartDateFormatter.getDate(datePicker);
            }
            return null;
        });

        Optional<LocalDate> result = dialog.showAndWait();
        result.ifPresent(newEnd -> {
            currentPeriod.setEndDate(newEnd);
            budgetService.updatePeriod(currentPeriod);
            refresh();
        });
    }

    private void updateSummary(BudgetPeriod period) {
        if (period == null) {
            totalBudgetedLabel.setText("0");
            totalSpentLabel.setText("0");
            totalRemainingLabel.setText("0");
            return;
        }

        List<CategoryBudget> budgets = budgetService.getBudgetsForPeriod(period);
        BigDecimal totalBudgeted = budgets.stream()
                .map(CategoryBudget::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = budgets.stream()
                .map(CategoryBudget::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemaining = totalBudgeted.subtract(totalSpent);

        String symbol = userSession.getCurrentUser().getCurrency().getSymbol();
        totalBudgetedLabel.setText(symbol + " " + totalBudgeted);
        totalSpentLabel.setText(symbol + " " + totalSpent);
        totalRemainingLabel.setText(symbol + " " + totalRemaining);
    }

    @FXML
    private void handleSwitchMode() {
        User u = userSession.getCurrentUser();
        AppMode newMode = u.getAppMode() == AppMode.PERIODIC ? AppMode.CONTINUOUS : AppMode.PERIODIC;
        if (newMode == AppMode.CONTINUOUS && currentPeriod != null) {
            budgetService.closeActivePeriod(u);
            u = userRepo.findByUsername(u.getUsername()).orElse(u);
            userSession.setCurrentUser(u);
            currentPeriod = null;
        }
        u.setAppMode(newMode);
        userRepo.update(u);
        userSession.setCurrentUser(u);
        nav.showAlert("Mode Changed", "App mode is now: " +
                (u.getAppMode() == AppMode.PERIODIC ? "Periodic (Envelopes)" : "Continuous"));
        refresh();
    }

    @FXML
    private void handleStartPeriod() {
        LocalDate start = SmartDateFormatter.getDate(startDatePicker);
        LocalDate end = SmartDateFormatter.getDate(endDatePicker);

        if (start == null || end == null) {
            nav.showAlert("Error", "Please enter valid Start and End dates.");
            return;
        }
        if (start.isAfter(end)) {
            nav.showAlert("Error", "Start date cannot be after end date.");
            return;
        }
        if (end.isBefore(LocalDate.now())) {
            nav.showAlert("Error", "End date must be in the future (today or later).");
            return;
        }

        User user = userSession.getCurrentUser();

        // Перевірка наявності минулих транзакцій за цей період
        boolean hasPastTransactions = false;
        List<BudgetPeriod> pastPeriods = budgetService.getAllPeriods(user);
        for (BudgetPeriod p : pastPeriods) {
            if (p.getStartDate().equals(start) && p.getEndDate().equals(end)) {
                List<Transaction> pastTx = financeService.getUserTransactions(user).stream()
                        .filter(t -> !t.getDate().isBefore(start) && !t.getDate().isAfter(end))
                        .toList();
                if (!pastTx.isEmpty()) {
                    hasPastTransactions = true;
                    break;
                }
            }
        }

        // Починаємо новий період (без імпорту)
        budgetService.startNewPeriod(user, start, end);
        refresh();

        if (hasPastTransactions) {
            nav.showAlert("Past Transactions Found",
                    "There are transactions from previous periods with the same date range.\n" +
                            "You can view them using filters on the Transactions page.\n" +
                            "The current period started fresh.");
        }
    }

    @FXML
    private void handleClosePeriod() {
        if (currentPeriod == null) return;

        User user = userSession.getCurrentUser();
        BigDecimal totalUnspent = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;

        String symbol = user.getCurrency().getSymbol();
        String message = "Unspent money from this period: " + symbol + " " + totalUnspent +
                "\n\nIt will be added to your Unallocated Savings.\nDo you want to close the period?";

        boolean confirm = nav.showConfirmation("Close Period", "Confirm Period Closing", message);

        if (confirm) {
            budgetService.closeActivePeriod(user);
            User updatedUser = userRepo.findByUsername(user.getUsername())
                    .orElse(user);
            userSession.setCurrentUser(updatedUser);
            refresh();
        }
    }

    @FXML
    private void handleSaveBudgets() {
        if (currentPeriod == null) return;
        for (Map.Entry<CategoryGroup, BudgetRowUI> entry : rowUIMap.entrySet()) {
            saveBudgetForGroup(entry.getKey(), entry.getValue().enableCheckBox, entry.getValue().allocInput);
        }
        nav.showAlert("Success", "All budgets saved!");
    }

    private void saveBudgetForGroup(CategoryGroup group, CheckBox enableCheckBox, TextField field) {
        if (enableCheckBox.isSelected()) {
            BigDecimal amount;
            try {
                String text = field.getText();
                amount = (text == null || text.isBlank()) ? BigDecimal.ZERO : new BigDecimal(text);
                if (amount.compareTo(BigDecimal.ZERO) < 0) {
                    nav.showAlert("Error", "Limit cannot be negative.");
                    return;
                }
            } catch (NumberFormatException e) {
                nav.showAlert("Error", "Invalid amount format.");
                return;
            }
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                budgetService.removeCategoryBudget(currentPeriod, group);
                updateBudgetRow(group, enableCheckBox, BigDecimal.ZERO);
            } else {
                try {
                    budgetService.setCategoryBudget(currentPeriod, group, amount);
                    updateBudgetRow(group, enableCheckBox, amount);
                } catch (IllegalArgumentException e) {
                    nav.showAlert("Error", e.getMessage());
                    // Відновити попередній стан
                    CategoryBudget existing = budgetService.getBudgetsForPeriod(currentPeriod).stream()
                            .filter(b -> b.getCategoryGroup().getId().equals(group.getId()))
                            .findFirst().orElse(null);
                    if (existing != null && existing.getAllocatedAmount().compareTo(BigDecimal.ZERO) > 0) {
                        field.setText(existing.getAllocatedAmount().toString());
                        enableCheckBox.setSelected(true);
                    } else {
                        field.setText("");
                        enableCheckBox.setSelected(false);
                        budgetService.removeCategoryBudget(currentPeriod, group);
                    }
                }
            }
        } else {
            budgetService.removeCategoryBudget(currentPeriod, group);
            updateBudgetRow(group, enableCheckBox, BigDecimal.ZERO);
        }
    }

    private void updateBudgetRow(CategoryGroup group, CheckBox enableCheckBox, BigDecimal newAllocated) {
        BudgetRowUI ui = rowUIMap.get(group);
        if (ui == null) return;

        CategoryBudget budget = budgetService.getBudgetsForPeriod(currentPeriod).stream()
                .filter(b -> b.getCategoryGroup().getId().equals(group.getId()))
                .findFirst().orElse(null);

        BigDecimal spent = (budget != null && budget.getSpentAmount() != null) ? budget.getSpentAmount() : BigDecimal.ZERO;
        String symbol = userSession.getCurrentUser().getCurrency().getSymbol();

        if (enableCheckBox.isSelected() && newAllocated.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remaining = newAllocated.subtract(spent);
            ui.spentRemainingLabel.setText("Spent: " + symbol + spent + " / Left: " + symbol + remaining);
            double progress = spent.divide(newAllocated, 2, RoundingMode.HALF_UP).doubleValue();
            ui.progressBar.setProgress(Math.min(progress, 1));
            ui.progressBar.getStyleClass().removeAll("progress-good", "progress-warning", "progress-danger");
            if (progress >= 1.0) ui.progressBar.getStyleClass().add("progress-danger");
            else if (progress >= 0.8) ui.progressBar.getStyleClass().add("progress-warning");
            else ui.progressBar.getStyleClass().add("progress-good");
        } else {
            ui.spentRemainingLabel.setText("Spent: " + symbol + spent + " / No limit");
            ui.progressBar.setProgress(0);
            ui.progressBar.getStyleClass().removeAll("progress-good", "progress-warning", "progress-danger");
            ui.progressBar.getStyleClass().add("progress-good");
        }

        updateSummary(currentPeriod);
    }

    private void buildBudgetRows() {
        User user = userSession.getCurrentUser();
        String symbol = user.getCurrency() != null ? user.getCurrency().getSymbol() : "$";

        List<CategoryGroup> groups = categoryService.getGroupsForUserByType(user, TransactionType.EXPENSE)
                .stream().filter(g -> !g.getName().equalsIgnoreCase("Savings") && !g.getName().equalsIgnoreCase("System")).toList();
        List<CategoryBudget> budgets = budgetService.getBudgetsForPeriod(currentPeriod);

        if (groups.isEmpty()) {
            Label noGroups = new Label("First create at least one expense category in the Categories page.");
            noGroups.getStyleClass().add("input-label");
            budgetsContainer.getChildren().add(noGroups);
            return;
        }

        for (CategoryGroup group : groups) {
            CategoryBudget budget = budgets.stream()
                    .filter(b -> b.getCategoryGroup().getId().equals(group.getId()))
                    .findFirst().orElse(null);

            boolean hasLimit = (budget != null && budget.getAllocatedAmount() != null && budget.getAllocatedAmount().compareTo(BigDecimal.ZERO) > 0);
            BigDecimal alloc = hasLimit ? budget.getAllocatedAmount() : BigDecimal.ZERO;
            BigDecimal spent = (budget != null && budget.getSpentAmount() != null) ? budget.getSpentAmount() : BigDecimal.ZERO;

            VBox card = new VBox(12);
            card.getStyleClass().add("budget-card");

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);

            Region colorDot = new Region();
            colorDot.setStyle("-fx-background-color: " + group.getColor() + "; -fx-min-width: 16px; -fx-min-height: 16px; -fx-background-radius: 8px;");
            Label groupName = new Label(group.getName());
            groupName.getStyleClass().add("budget-group-name");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label spentRemainingLabel = new Label(hasLimit ? "Spent: " + symbol + spent + " / Left: " + symbol + alloc.subtract(spent)
                    : "Spent: " + symbol + spent + " / No limit");
            spentRemainingLabel.getStyleClass().add("budget-spent-remaining");

            header.getChildren().addAll(colorDot, groupName, spacer, spentRemainingLabel);

            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);

            CheckBox enableCheckBox = new CheckBox("Enable limit");
            enableCheckBox.setSelected(hasLimit);
            enableCheckBox.getStyleClass().add("budget-checkbox");

            Label limitLabel = new Label("Limit:");
            limitLabel.getStyleClass().add("budget-limit-label");

            TextField allocInput = new TextField(hasLimit ? alloc.toString() : "");
            allocInput.setPromptText("0.00");
            allocInput.setPrefWidth(120);
            allocInput.getStyleClass().addAll("text-field", "budget-input");
            allocInput.setDisable(!hasLimit);

            enableCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                allocInput.setDisable(!newVal);
                if (!newVal) {
                    allocInput.setText("");
                } else if (allocInput.getText().isEmpty()) {
                    allocInput.setText("0");
                }
            });

            double progress = hasLimit ? spent.divide(alloc, 2, RoundingMode.HALF_UP).doubleValue() : 0;
            ProgressBar pb = new ProgressBar(progress);
            pb.setMaxWidth(Double.MAX_VALUE);
            pb.getStyleClass().add("budget-progress-bar");
            HBox.setHgrow(pb, Priority.ALWAYS);
            if (hasLimit) {
                if (progress >= 1.0) pb.getStyleClass().add("progress-danger");
                else if (progress >= 0.8) pb.getStyleClass().add("progress-warning");
                else pb.getStyleClass().add("progress-good");
            } else {
                pb.getStyleClass().add("progress-good");
            }

            row.getChildren().addAll(enableCheckBox, limitLabel, allocInput, pb);
            card.getChildren().addAll(header, row);
            budgetsContainer.getChildren().add(card);

            BudgetRowUI ui = new BudgetRowUI();
            ui.enableCheckBox = enableCheckBox;
            ui.allocInput = allocInput;
            ui.spentRemainingLabel = spentRemainingLabel;
            ui.progressBar = pb;
            rowUIMap.put(group, ui);

            allocInput.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    saveBudgetForGroup(group, enableCheckBox, allocInput);
                }
            });
            enableCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                saveBudgetForGroup(group, enableCheckBox, allocInput);
            });
        }
    }
}