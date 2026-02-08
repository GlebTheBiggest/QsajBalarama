package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.SavingsGoal;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.service.SavingsService;
import com.qsaj.balarama.v1.session.UserSession;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SavingsController {

    @FXML private TextField searchGoalField; // Поле пошуку
    @FXML private TextField goalNameField;
    @FXML private TextField targetAmountField;
    @FXML private FlowPane goalsContainer;

    private final UserSession userSession;
    private final SavingsService savingsService;

    private List<SavingsGoal> allGoals;

    @Inject
    public SavingsController(UserSession userSession, SavingsService savingsService) {
        this.userSession = userSession;
        this.savingsService = savingsService;
    }

    @FXML
    public void initialize() {
        loadGoals();
        searchGoalField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterGoals(newVal);
        });
    }

    private void loadGoals() {
        User user = userSession.getCurrentUser();
        allGoals = savingsService.getUserGoals(user);
        renderGoals(allGoals);
    }

    private void filterGoals(String query) {
        if (query == null || query.isEmpty()) {
            renderGoals(allGoals);
        } else {
            List<SavingsGoal> filtered = allGoals.stream()
                    .filter(g -> g.getName().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
            renderGoals(filtered);
        }
    }

    private void renderGoals(List<SavingsGoal> goals) {
        goalsContainer.getChildren().clear();
        User user = userSession.getCurrentUser();
        String symbol = (user.getCurrency() != null) ? user.getCurrency().getSymbol() : "$";

        if (goals.isEmpty()) {
            Label emptyLabel = new Label("No savings goals found.");
            emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14px;");
            goalsContainer.getChildren().add(emptyLabel);
        } else {
            for (SavingsGoal goal : goals) {
                goalsContainer.getChildren().add(createGoalCard(goal, symbol));
            }
        }
    }

    @FXML
    private void handleCreateGoal() {
        try {
            String name = goalNameField.getText();
            String amountStr = targetAmountField.getText();

            if (name.isEmpty() || amountStr.isEmpty()) {
                showAlert("Error", "Please fill in all fields");
                return;
            }

            BigDecimal target = new BigDecimal(amountStr);
            savingsService.createGoal(userSession.getCurrentUser(), name, target);

            goalNameField.clear();
            targetAmountField.clear();

            loadGoals(); // Перезавантажуємо список

        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid amount format");
        }
    }

    // --- ГЕНЕРАЦІЯ КАРТКИ UI ---
    private VBox createGoalCard(SavingsGoal goal, String symbol) {
        VBox card = new VBox(15);
        card.getStyleClass().add("goal-card"); // CSS клас з savings.css

        // 1. Заголовок і кнопка видалення
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(goal.getName());
        title.getStyleClass().add("goal-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteBtn = new Button("X");
        deleteBtn.getTooltip();
        deleteBtn.setTooltip(new Tooltip("Delete Goal"));
        deleteBtn.getStyleClass().add("button-delete-icon");
        deleteBtn.setOnAction(e -> handleDeleteGoal(goal));

        header.getChildren().addAll(title, spacer, deleteBtn);

        // 2. Текст прогресу
        Label progressText = new Label(symbol + " " + goal.getCurrentAmount() + " / " + symbol + " " + goal.getTargetAmount());
        progressText.getStyleClass().add("goal-progress-text");

        // 3. Прогрес бар
        ProgressBar progressBar = new ProgressBar(goal.getProgress());
        progressBar.setMaxWidth(Double.MAX_VALUE);

        // Колір: Зелений, якщо виконано, Синій - якщо в процесі
        if (goal.getProgress() >= 1.0) {
            progressBar.setStyle("-fx-accent: #27ae60;");
        } else {
            progressBar.setStyle("-fx-accent: #3498db;");
        }

        // 4. Кнопка "Додати кошти"
        Button addFundsBtn = new Button("Add Funds");
        addFundsBtn.setMaxWidth(Double.MAX_VALUE);
        addFundsBtn.getStyleClass().add("button-add-funds");

        if (goal.getProgress() >= 1.0) {
            addFundsBtn.setText("Goal Reached! 🎉");
            addFundsBtn.setDisable(true);
        } else {
            addFundsBtn.setOnAction(e -> showAddFundsDialog(goal));
        }

        card.getChildren().addAll(header, progressText, progressBar, addFundsBtn);
        return card;
    }

    private void handleDeleteGoal(SavingsGoal goal) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Goal");
        alert.setHeaderText("Remove '" + goal.getName() + "'?");
        alert.setContentText("Funds (" + goal.getCurrentAmount() + ") will be returned to your main balance.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            savingsService.deleteGoal(goal.getId(), userSession.getCurrentUser());
            loadGoals();
        }
    }

    private void showAddFundsDialog(SavingsGoal goal) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Funds");
        dialog.setHeaderText("Add to: " + goal.getName());
        dialog.setContentText("Amount:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(amountStr -> {
            try {
                BigDecimal amount = new BigDecimal(amountStr);
                savingsService.addFundsToGoal(goal.getId(), amount, userSession.getCurrentUser());
                loadGoals();
                showAlert("Success", "Funds added!");
            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid number");
            } catch (IllegalArgumentException e) {
                showAlert("Error", e.getMessage());
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}