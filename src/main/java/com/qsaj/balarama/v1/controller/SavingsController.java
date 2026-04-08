package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.*;
import com.qsaj.balarama.v1.service.BudgetService;
import com.qsaj.balarama.v1.service.NavigationService;
import com.qsaj.balarama.v1.service.SavingsService;
import com.qsaj.balarama.v1.service.impl.CategoryServiceImpl;
import com.qsaj.balarama.v1.session.UserSession;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SavingsController {

    @FXML private TextField searchNameField;
    @FXML private TextField minTargetField;
    @FXML private TextField maxTargetField;
    @FXML private CheckBox showCompletedCheckbox;

    @FXML private Label totalSavingsLabel;
    @FXML private Label unallocatedSavingsLabel;
    @FXML private Button transferFromUnallocatedBtn;
    @FXML private Button transferToMainBtn;

    @FXML private TextField goalNameField;
    @FXML private TextField targetAmountField;
    @FXML private FlowPane goalsContainer;

    private final UserSession userSession;
    private final SavingsService savingsService;
    private final NavigationService navigationService;
    private final BudgetService budgetService;
    private final CategoryServiceImpl categoryService;

    private List<SavingsGoal> allGoals;
    private BigDecimal unallocatedSavings;

    @Inject
    public SavingsController(UserSession userSession, SavingsService savingsService,
                             NavigationService navigationService, BudgetService budgetService,
                             CategoryServiceImpl categoryService) {
        this.userSession = userSession;
        this.savingsService = savingsService;
        this.navigationService = navigationService;
        this.budgetService = budgetService;
        this.categoryService = categoryService;
    }

    @FXML
    public void initialize() {
        searchNameField.textProperty().addListener((o, old, val) -> applyFilters());
        minTargetField.textProperty().addListener((o, old, val) -> applyFilters());
        maxTargetField.textProperty().addListener((o, old, val) -> applyFilters());
        showCompletedCheckbox.selectedProperty().addListener((o, old, val) -> applyFilters());

        loadData();
        transferFromUnallocatedBtn.setOnAction(e -> showTransferFromUnallocatedDialog());
        transferToMainBtn.setOnAction(e -> showTransferToMainDialog());
    }

    @FXML
    private void handleResetFilter() {
        searchNameField.clear();
        minTargetField.clear();
        maxTargetField.clear();
        showCompletedCheckbox.setSelected(false);
    }

    private void loadData() {
        User user = userSession.getCurrentUser();
        unallocatedSavings = user.getUnallocatedSavings() != null ? user.getUnallocatedSavings() : BigDecimal.ZERO;
        allGoals = savingsService.getUserGoals(user);

        String symbol = user.getCurrency().getSymbol();
        BigDecimal totalSavings = savingsService.getTotalSavings(user);
        totalSavingsLabel.setText(symbol + " " + totalSavings);
        unallocatedSavingsLabel.setText(symbol + " " + unallocatedSavings);

        // Блокуємо кнопки, якщо немає нерозподілених коштів
        boolean hasUnallocated = unallocatedSavings.compareTo(BigDecimal.ZERO) > 0;
        transferFromUnallocatedBtn.setDisable(!hasUnallocated);
        transferToMainBtn.setDisable(!hasUnallocated);

        applyFilters();
    }

    private void applyFilters() {
        String nameQuery = searchNameField.getText().toLowerCase();
        BigDecimal min = parseBigDecimal(minTargetField.getText());
        BigDecimal max = parseBigDecimal(maxTargetField.getText());
        boolean showCompleted = showCompletedCheckbox.isSelected();

        List<SavingsGoal> filtered = allGoals.stream()
                .filter(g -> nameQuery.isEmpty() || g.getName().toLowerCase().contains(nameQuery))
                .filter(g -> min == null || g.getTargetAmount().compareTo(min) >= 0)
                .filter(g -> max == null || g.getTargetAmount().compareTo(max) <= 0)
                .filter(g -> showCompleted || !g.isCompleted())
                .collect(Collectors.toList());
        renderGoals(filtered);
    }

    private BigDecimal parseBigDecimal(String text) {
        try { return text == null || text.isBlank() ? null : new BigDecimal(text); }
        catch (Exception e) { return null; }
    }

    private void renderGoals(List<SavingsGoal> goals) {
        goalsContainer.getChildren().clear();
        String symbol = userSession.getCurrentUser().getCurrency().getSymbol();

        for (SavingsGoal goal : goals) {
            goalsContainer.getChildren().add(createGoalCard(goal, symbol));
        }
        if (goals.isEmpty()) {
            Label empty = new Label("No savings goals found.");
            empty.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14px;");
            goalsContainer.getChildren().add(empty);
        }
    }

    @FXML
    private void handleCreateGoal() {
        try {
            String name = goalNameField.getText();
            String amountStr = targetAmountField.getText();
            if (name.isBlank() || amountStr.isBlank()) {
                navigationService.showAlert("Error", "Please fill in all fields");
                return;
            }
            savingsService.createGoal(userSession.getCurrentUser(), name, new BigDecimal(amountStr));
            goalNameField.clear();
            targetAmountField.clear();
            loadData();
        } catch (NumberFormatException e) {
            navigationService.showAlert("Error", "Invalid amount format");
        }
    }

    private VBox createGoalCard(SavingsGoal goal, String symbol) {
        VBox card = new VBox(12);
        card.getStyleClass().add("goal-card");
        if (goal.isCompleted()) {
            card.setStyle("-fx-opacity: 0.7; -fx-background-color: #2c3e50;");
        }

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(goal.getName());
        title.getStyleClass().add("goal-title");
        if (goal.isCompleted()) {
            title.setText(title.getText() + " (Completed)");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteBtn = new Button("✖");
        deleteBtn.getStyleClass().add("button-delete-icon");
        deleteBtn.setOnAction(e -> {
            if (navigationService.showConfirmation("Delete Goal", "Remove '" + goal.getName() + "'?", "Funds will return to unallocated savings.")) {
                savingsService.deleteGoal(goal.getId(), userSession.getCurrentUser());
                loadData();
            }
        });

        header.getChildren().addAll(title, spacer, deleteBtn);

        Label progressText = new Label(symbol + " " + goal.getCurrentAmount() + " / " + symbol + " " + goal.getTargetAmount());
        progressText.getStyleClass().add("goal-progress-text");

        ProgressBar pb = new ProgressBar(goal.getProgress());
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setStyle("-fx-accent: " + (goal.getProgress() >= 1.0 ? "#27ae60;" : "#3498db;"));

        VBox buttonBox = new VBox(8);
        buttonBox.setFillWidth(true);

        Button addBtn = new Button("Add Funds");
        addBtn.getStyleClass().add("button-add-funds");
        addBtn.setMaxWidth(Double.MAX_VALUE);

        Button transferBtn = new Button("Transfer");
        transferBtn.getStyleClass().add("button-transfer-back");
        transferBtn.setMaxWidth(Double.MAX_VALUE);

        Button completeBtn = new Button("Complete");
        completeBtn.getStyleClass().add("button-primary");
        completeBtn.setMaxWidth(Double.MAX_VALUE);

        if (goal.isCompleted()) {
            addBtn.setDisable(true);
            transferBtn.setDisable(true);
            completeBtn.setDisable(true);
            completeBtn.setText("Completed");
        } else {
            addBtn.setOnAction(e -> showAddFundsDialog(goal));
            transferBtn.setOnAction(e -> showTransferMenu(goal, transferBtn));
            if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
                completeBtn.setOnAction(e -> showCompleteDialog(goal));
            } else {
                completeBtn.setDisable(true);
                completeBtn.setText("Not reached");
            }
        }

        buttonBox.getChildren().addAll(addBtn, transferBtn, completeBtn);
        card.getChildren().addAll(header, progressText, pb, buttonBox);
        return card;
    }

    private void showTransferMenu(SavingsGoal goal, Button anchor) {
        ContextMenu menu = new ContextMenu();
        MenuItem toUnallocated = new MenuItem("To Unallocated Savings");
        MenuItem toMain = new MenuItem("To Main Balance");
        MenuItem toAnother = new MenuItem("To Another Goal");

        toUnallocated.setOnAction(e -> showTransferToUnallocatedDialog(goal));
        toMain.setOnAction(e -> showTransferToMainFromGoalDialog(goal));
        toAnother.setOnAction(e -> showTransferBetweenGoalsDialog(goal));

        menu.getItems().addAll(toUnallocated, toMain, toAnother);
        menu.show(anchor, anchor.getScene().getWindow().getX() + anchor.localToScreen(anchor.getBoundsInLocal()).getMinX(),
                anchor.getScene().getWindow().getY() + anchor.localToScreen(anchor.getBoundsInLocal()).getMaxY());
    }

    private void showTransferToUnallocatedDialog(SavingsGoal goal) {
        if (goal.getCurrentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            navigationService.showAlert("No funds", "This goal has no funds to transfer.");
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Transfer to Unallocated");
        dialog.setHeaderText("Transfer from '" + goal.getName() + "' to Unallocated Savings");
        TextField amountField = new TextField();
        amountField.setPromptText("Max: " + goal.getCurrentAmount());
        dialog.getDialogPane().setContent(new VBox(10, new Label("Amount:"), amountField));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialog(dialog.getDialogPane());

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    BigDecimal amount = new BigDecimal(amountField.getText());
                    if (amount.compareTo(goal.getCurrentAmount()) > 0) throw new IllegalArgumentException();
                    savingsService.transferFromGoalToUnallocated(userSession.getCurrentUser(), goal.getId(), amount);
                    loadData();
                    navigationService.showAlert("Success", "Transferred to unallocated savings!");
                } catch (Exception ex) {
                    navigationService.showAlert("Error", "Invalid amount.");
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void showTransferToMainFromGoalDialog(SavingsGoal goal) {
        if (goal.getCurrentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            navigationService.showAlert("No funds", "This goal has no funds to transfer.");
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Transfer to Main Balance");
        dialog.setHeaderText("Transfer from '" + goal.getName() + "' to Main Balance");
        TextField amountField = new TextField();
        amountField.setPromptText("Max: " + goal.getCurrentAmount());
        dialog.getDialogPane().setContent(new VBox(10, new Label("Amount:"), amountField));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialog(dialog.getDialogPane());

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    BigDecimal amount = new BigDecimal(amountField.getText());
                    if (amount.compareTo(goal.getCurrentAmount()) > 0) throw new IllegalArgumentException();
                    savingsService.transferFromGoalToMain(userSession.getCurrentUser(), goal.getId(), amount);
                    loadData();
                    navigationService.showAlert("Success", "Transferred to main balance!");
                } catch (Exception ex) {
                    navigationService.showAlert("Error", "Invalid amount.");
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void showTransferBetweenGoalsDialog(SavingsGoal fromGoal) {
        if (fromGoal.getCurrentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            navigationService.showAlert("No funds", "This goal has no funds to transfer.");
            return;
        }
        List<SavingsGoal> otherGoals = allGoals.stream()
                .filter(g -> !g.getId().equals(fromGoal.getId()) && !g.isCompleted())
                .toList();
        if (otherGoals.isEmpty()) {
            navigationService.showAlert("No destination", "No other active goals to transfer to.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Transfer to Another Goal");
        dialog.setHeaderText("Transfer from '" + fromGoal.getName() + "' to another goal");

        ComboBox<SavingsGoal> goalCombo = new ComboBox<>();
        goalCombo.getItems().addAll(otherGoals);
        goalCombo.setConverter(new javafx.util.StringConverter<SavingsGoal>() {
            @Override public String toString(SavingsGoal g) { return g == null ? "" : g.getName(); }
            @Override public SavingsGoal fromString(String s) { return null; }
        });
        TextField amountField = new TextField();
        amountField.setPromptText("Max: " + fromGoal.getCurrentAmount());

        VBox content = new VBox(10, new Label("Destination goal:"), goalCombo, new Label("Amount:"), amountField);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialog(dialog.getDialogPane());

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && goalCombo.getValue() != null) {
                try {
                    BigDecimal amount = new BigDecimal(amountField.getText());
                    if (amount.compareTo(fromGoal.getCurrentAmount()) > 0) throw new IllegalArgumentException();
                    savingsService.transferBetweenGoals(userSession.getCurrentUser(),
                            fromGoal.getId(), goalCombo.getValue().getId(), amount);
                    loadData();
                    navigationService.showAlert("Success", "Transferred to " + goalCombo.getValue().getName());
                } catch (Exception e) {
                    navigationService.showAlert("Error", "Invalid amount.");
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    // ===================== ВИПРАВЛЕНИЙ МЕТОД ДЛЯ ДОДАВАННЯ КОШТІВ =====================
    private void showAddFundsDialog(SavingsGoal goal) {
        boolean hasUnallocated = unallocatedSavings != null && unallocatedSavings.compareTo(BigDecimal.ZERO) > 0;

        if (!hasUnallocated) {
            // Немає нерозподілених – одразу беремо з головного балансу
            showAddFundsDirectFromMain(goal);
            return;
        }

        // Є нерозподілені – питаємо джерело
        ChoiceDialog<String> sourceDialog = new ChoiceDialog<>("From Main Balance", "From Main Balance", "From Unallocated Savings");
        sourceDialog.setTitle("Add Funds");
        sourceDialog.setHeaderText("Choose source for adding funds to '" + goal.getName() + "'");
        sourceDialog.setContentText("Source:");
        Optional<String> sourceResult = sourceDialog.showAndWait();
        if (sourceResult.isEmpty()) return;

        if ("From Main Balance".equals(sourceResult.get())) {
            showAddFundsDirectFromMain(goal);
        } else {
            showAddFundsFromUnallocated(goal);
        }
    }

    private void showAddFundsDirectFromMain(SavingsGoal goal) {
        Dialog<BigDecimal> amountDialog = new Dialog<>();
        amountDialog.setTitle("Amount");
        amountDialog.setHeaderText("Enter amount to add from Main Balance");
        TextField amountField = new TextField();
        amountDialog.getDialogPane().setContent(new VBox(10, new Label("Amount:"), amountField));
        amountDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialog(amountDialog.getDialogPane());

        amountDialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    return new BigDecimal(amountField.getText());
                } catch (Exception e) { return null; }
            }
            return null;
        });

        Optional<BigDecimal> amountResult = amountDialog.showAndWait();
        if (amountResult.isEmpty() || amountResult.get().compareTo(BigDecimal.ZERO) <= 0) return;

        try {
            savingsService.addFundsToGoalFromBudget(userSession.getCurrentUser(), goal.getId(), amountResult.get());
            loadData();
            navigationService.showAlert("Success", "Funds added to goal from main balance!");
        } catch (Exception e) {
            navigationService.showAlert("Error", e.getMessage());
        }
    }

    private void showAddFundsFromUnallocated(SavingsGoal goal) {
        // Перевірка ще раз (на випадок, якщо гроші закінчилися між діалогами)
        if (unallocatedSavings.compareTo(BigDecimal.ZERO) <= 0) {
            navigationService.showAlert("No funds", "No unallocated savings available.");
            return;
        }

        Dialog<BigDecimal> amountDialog = new Dialog<>();
        amountDialog.setTitle("Amount");
        amountDialog.setHeaderText("Enter amount to add from Unallocated Savings (available: " +
                userSession.getCurrentUser().getCurrency().getSymbol() + " " + unallocatedSavings + ")");
        TextField amountField = new TextField();
        amountDialog.getDialogPane().setContent(new VBox(10, new Label("Amount:"), amountField));
        amountDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialog(amountDialog.getDialogPane());

        amountDialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    return new BigDecimal(amountField.getText());
                } catch (Exception e) { return null; }
            }
            return null;
        });

        Optional<BigDecimal> amountResult = amountDialog.showAndWait();
        if (amountResult.isEmpty() || amountResult.get().compareTo(BigDecimal.ZERO) <= 0) return;

        try {
            savingsService.addFundsToGoalFromUnallocated(userSession.getCurrentUser(), goal.getId(), amountResult.get());
            loadData();
            navigationService.showAlert("Success", "Funds added to goal from unallocated savings!");
        } catch (Exception e) {
            navigationService.showAlert("Error", e.getMessage());
        }
    }
    // ========================================================================

    private void showCompleteDialog(SavingsGoal goal) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Complete Goal");
        dialog.setHeaderText("Congratulations! You've reached your goal: " + goal.getName());

        TextArea noteArea = new TextArea();
        noteArea.setPromptText("Add a note about this achievement (optional)");
        noteArea.setPrefRowCount(3);

        String stats = "Total deposits: " + goal.getTotalTransactions() + "\n" +
                "Total withdrawals before completion: " + goal.getTotalWithdrawals() + "\n" +
                "Days to complete: " + goal.getDaysToComplete() + "\n";
        Label statsLabel = new Label(stats);
        statsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ecf0f1;");

        VBox content = new VBox(10, statsLabel, new Label("Note:"), noteArea);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialog(dialog.getDialogPane());

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? noteArea.getText() : null);
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                savingsService.completeGoal(goal.getId(), userSession.getCurrentUser(), result.get());
                loadData();
                navigationService.showAlert("Success", "Goal marked as completed!");
            } catch (Exception e) {
                navigationService.showAlert("Error", e.getMessage());
            }
        }
    }

    private void showTransferFromUnallocatedDialog() {
        if (unallocatedSavings.compareTo(BigDecimal.ZERO) <= 0) {
            navigationService.showAlert("No funds", "No unallocated savings to transfer.");
            return;
        }
        List<SavingsGoal> activeGoals = allGoals.stream().filter(g -> !g.isCompleted()).toList();
        if (activeGoals.isEmpty()) {
            navigationService.showAlert("No goals", "Create a goal first.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Transfer from Unallocated");
        dialog.setHeaderText("Available: " + userSession.getCurrentUser().getCurrency().getSymbol() + " " + unallocatedSavings);

        ComboBox<SavingsGoal> goalCombo = new ComboBox<>();
        goalCombo.getItems().addAll(activeGoals);
        goalCombo.setConverter(new javafx.util.StringConverter<SavingsGoal>() {
            @Override public String toString(SavingsGoal g) { return g == null ? "" : g.getName(); }
            @Override public SavingsGoal fromString(String s) { return null; }
        });
        TextField amountField = new TextField();

        VBox content = new VBox(10, new Label("Goal:"), goalCombo, new Label("Amount:"), amountField);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialog(dialog.getDialogPane());

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && goalCombo.getValue() != null) {
                try {
                    BigDecimal amount = new BigDecimal(amountField.getText());
                    savingsService.addFundsToGoalFromUnallocated(userSession.getCurrentUser(),
                            goalCombo.getValue().getId(), amount);
                    loadData();
                    navigationService.showAlert("Success", "Funds transferred!");
                } catch (Exception e) {
                    navigationService.showAlert("Error", e.getMessage());
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void showTransferToMainDialog() {
        if (unallocatedSavings.compareTo(BigDecimal.ZERO) <= 0) {
            navigationService.showAlert("No funds", "No unallocated savings to transfer.");
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Transfer to Main Balance");
        dialog.setHeaderText("Available: " + userSession.getCurrentUser().getCurrency().getSymbol() + " " + unallocatedSavings);
        TextField amountField = new TextField();
        dialog.getDialogPane().setContent(new VBox(10, new Label("Amount:"), amountField));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialog(dialog.getDialogPane());

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    BigDecimal amount = new BigDecimal(amountField.getText());
                    savingsService.transferFromUnallocatedToMain(userSession.getCurrentUser(), amount);
                    loadData();
                    navigationService.showAlert("Success", "Transferred to main balance!");
                } catch (Exception e) {
                    navigationService.showAlert("Error", e.getMessage());
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void styleDialog(DialogPane pane) {
        pane.getStylesheets().clear();
        pane.getStylesheets().addAll(Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm(),
                Objects.requireNonNull(getClass().getResource("/css/dashboard.css")).toExternalForm());
        pane.getStyleClass().add("dialog-pane");
    }
}