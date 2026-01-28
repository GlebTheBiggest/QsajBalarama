package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.Category;
import com.qsaj.balarama.v1.model.enums.Currency;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.service.FinanceService;
import com.qsaj.balarama.v1.session.UserSession;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionsController {

    @FXML private TextField titleField;
    @FXML private TextField amountField;
    @FXML private ComboBox<TransactionType> typeBox;
    @FXML private ComboBox<Category> categoryBox;
    @FXML private DatePicker datePicker;

    // Statistics Labels
    @FXML private Label totalBalanceLabel;
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpenseLabel;

    @FXML private TableView<Transaction> transactionsTable;
    @FXML private TableColumn<Transaction, LocalDate> colDate;
    @FXML private TableColumn<Transaction, String> colTitle;
    @FXML private TableColumn<Transaction, String> colCategory;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, BigDecimal> colAmount;
    @FXML private TableColumn<Transaction, Void> colAction;

    private final UserSession userSession;
    private final FinanceService financeService;

    @Inject
    public TransactionsController(UserSession userSession, FinanceService financeService) {
        this.userSession = userSession;
        this.financeService = financeService;
    }

    @FXML
    public void initialize() {
        setupTableColumns();

        // 1. Configure Type and Categories
        typeBox.getItems().setAll(TransactionType.values());

        // Listener: When Type changes -> Filter Categories
        typeBox.valueProperty().addListener((obs, oldType, newType) -> {
            if (newType != null) {
                List<Category> filteredCategories = Arrays.stream(Category.values())
                        .filter(cat -> cat.getType() == newType)
                        .collect(Collectors.toList());
                categoryBox.getItems().setAll(filteredCategories);
                categoryBox.getSelectionModel().selectFirst();
            }
        });

        // Default values
        datePicker.setValue(LocalDate.now());
        typeBox.setValue(TransactionType.EXPENSE); // Triggers listener to populate categories

        loadData();
    }

    private void setupTableColumns() {
        Currency userCurrency = getUserCurrency();
        String symbol = userCurrency.getSymbol();

        colDate.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDate()));
        colTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        colCategory.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory().name()));
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType().name()));

        // Sorting Fix: Use the raw number (BigDecimal)
        colAmount.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getAmount()));

        // Visual Formatting: Add colors and symbols
        colAmount.setCellFactory(column -> new TableCell<Transaction, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);

                if (empty || amount == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // Get the transaction to check if it's Income or Expense
                    Transaction t = getTableView().getItems().get(getIndex());

                    String prefix = (t.getType() == TransactionType.INCOME) ? "+ " : "- ";
                    setText(prefix + symbol + " " + amount);

                    if (t.getType() == TransactionType.INCOME) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                    }
                }
            }
        });

        // --- ЛОГІКА КНОПКИ ВИДАЛЕННЯ ---
        colAction.setCellFactory(param -> new TableCell<Transaction, Void>() {
            private final Button deleteBtn = new Button("X");

            {
                // Стилі кнопки (червона, маленька)
                deleteBtn.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
                deleteBtn.setOnAction(event -> {
                    Transaction t = getTableView().getItems().get(getIndex());
                    handleDeleteTransaction(t);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });
    }

    private void handleDeleteTransaction(Transaction t) {
        // Питаємо підтвердження
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Transaction");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("This will affect your balance: " + t.getTitle() + " (" + t.getAmount() + ")");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                User user = userSession.getCurrentUser();
                financeService.deleteTransaction(t.getId(), user);
                loadData();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error deleting transaction");
            }
        }
    }

    private void loadData() {
        User user = userSession.getCurrentUser();
        List<Transaction> list = financeService.getUserTransactions(user);
        transactionsTable.setItems(FXCollections.observableArrayList(list));

        calculateAndShowStats(list, user);
    }

    private void calculateAndShowStats(List<Transaction> transactions, User user) {
        Currency currency = getUserCurrency();
        String symbol = currency.getSymbol();

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.INCOME) {
                income = income.add(t.getAmount());
            } else {
                expense = expense.add(t.getAmount());
            }
        }

        BigDecimal balance = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;

        totalBalanceLabel.setText(symbol + " " + balance.toString());
        totalIncomeLabel.setText("+ " + symbol + " " + income.toString());
        totalExpenseLabel.setText("- " + symbol + " " + expense.toString());
    }

    private Currency getUserCurrency() {
        User user = userSession.getCurrentUser();
        return (user.getCurrency() != null) ? user.getCurrency() : Currency.USD;
    }

    @FXML
    private void handleAddTransaction() {
        try {
            String title = titleField.getText();
            String amountStr = amountField.getText();
            TransactionType type = typeBox.getValue();
            Category cat = categoryBox.getValue();
            LocalDate date = datePicker.getValue();

            if (title.isEmpty() || amountStr.isEmpty() || type == null || cat == null || date == null) {
                showAlert("Please fill in all fields");
                return;
            }

            BigDecimal amount = new BigDecimal(amountStr);
            User user = userSession.getCurrentUser();

            financeService.addTransaction(user, title, amount, type, cat, date);

            titleField.clear();
            amountField.clear();

            loadData();

        } catch (NumberFormatException e) {
            showAlert("Invalid amount format");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Could not save transaction");
        }
    }

    private void showAlert(String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}