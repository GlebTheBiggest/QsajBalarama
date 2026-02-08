package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.Transaction;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.*;
import com.qsaj.balarama.v1.service.FinanceService;
import com.qsaj.balarama.v1.service.NavigationService;
import com.qsaj.balarama.v1.session.UserSession;
import com.qsaj.balarama.v1.util.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class TransactionsController {

    @FXML private TextField titleField;
    @FXML private TextField amountField;
    @FXML private ComboBox<TransactionType> typeBox;
    @FXML private ComboBox<Category> categoryBox;
    @FXML private ComboBox<TransactionStatus> statusBox;
    @FXML private ComboBox<Priority> priorityBox;
    @FXML private DatePicker datePicker;
    @FXML private DatePicker dueDatePicker;
    @FXML private TextArea notesArea;

    @FXML private Label totalBalanceLabel;
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpenseLabel;

    @FXML private DatePicker filterStartDate;
    @FXML private DatePicker filterEndDate;
    @FXML private ComboBox<TransactionStatus> filterStatus;
    @FXML private ComboBox<Category> filterCategory;
    @FXML private TextField filterMinAmount;
    @FXML private TextField filterMaxAmount;
    @FXML private TextField searchField;

    @FXML private TableView<Transaction> transactionsTable;
    @FXML private TableColumn<Transaction, LocalDate> colDate;
    @FXML private TableColumn<Transaction, String> colTitle;
    @FXML private TableColumn<Transaction, String> colCategory;
    @FXML private TableColumn<Transaction, String> colStatus;
    @FXML private TableColumn<Transaction, LocalDate> colDueDate;
    @FXML private TableColumn<Transaction, String> colNotes;
    @FXML private TableColumn<Transaction, BigDecimal> colAmount;
    @FXML private TableColumn<Transaction, Void> colAction;

    private final UserSession userSession;
    private final FinanceService financeService;
    private final NavigationService navigationService;

    private final ObservableList<Transaction> masterData = FXCollections.observableArrayList();
    private final TransactionFilter transactionFilter;

    @Inject
    public TransactionsController(UserSession userSession, FinanceService financeService, NavigationService navigationService) {
        this.userSession = userSession;
        this.financeService = financeService;
        this.navigationService = navigationService;
        this.transactionFilter = new TransactionFilter();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        initializeComboBoxes();
        setupTypeChangeListener();
        setupFilterListeners();
        loadData();
    }

    private void setupTableColumns() {
        colDate.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDate()));

        colTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        colTitle.setCellFactory(column -> new TransactionTitleCell());

        colCategory.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory().name()));

        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        colStatus.setCellFactory(column -> new StatusTableCell());

        colDueDate.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDueDate()));

        colNotes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNotes()));
        colNotes.setCellFactory(column -> new NotesTableCell());

        colAmount.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getAmount()));
        colAmount.setCellFactory(column -> new AmountTableCell());

        colAction.setCellFactory(column -> new ActionTableCell(this::handleDeleteTransaction));
    }

    private void initializeComboBoxes() {
        typeBox.getItems().setAll(TransactionType.values());
        statusBox.getItems().setAll(TransactionStatus.values());
        priorityBox.getItems().setAll(Priority.values());
        filterStatus.getItems().setAll(TransactionStatus.values());
        filterCategory.getItems().setAll(Category.values());
    }

    private void setupTypeChangeListener() {
        typeBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                List<Category> filteredCategories = Arrays.stream(Category.values())
                        .filter(cat -> cat.getType() == newVal)
                        .toList();
                categoryBox.getItems().setAll(filteredCategories);
                categoryBox.getSelectionModel().selectFirst();

                if (newVal == TransactionType.INCOME) {
                    dueDatePicker.setValue(null);
                    dueDatePicker.setDisable(true);
                } else {
                    dueDatePicker.setDisable(false);
                }
            }
        });
    }

    private void setupFilterListeners() {
        filterStartDate.valueProperty().addListener(o -> applyFilters());
        filterEndDate.valueProperty().addListener(o -> applyFilters());
        filterStatus.valueProperty().addListener(o -> applyFilters());
        filterCategory.valueProperty().addListener(o -> applyFilters());
        searchField.textProperty().addListener(o -> applyFilters());
        filterMinAmount.textProperty().addListener(o -> applyFilters());
        filterMaxAmount.textProperty().addListener(o -> applyFilters());
    }

    private void loadData() {
        User user = userSession.getCurrentUser();
        List<Transaction> list = financeService.getUserTransactions(user);
        masterData.setAll(list);
        applyFilters();
    }

    private void applyFilters() {
        transactionFilter.setStartDate(filterStartDate.getValue());
        transactionFilter.setEndDate(filterEndDate.getValue());
        transactionFilter.setStatus(filterStatus.getValue());
        transactionFilter.setCategory(filterCategory.getValue());
        transactionFilter.setSearchText(searchField.getText());
        transactionFilter.setMinAmount(parseBigDecimal(filterMinAmount.getText()));
        transactionFilter.setMaxAmount(parseBigDecimal(filterMaxAmount.getText()));

        List<Transaction> filtered = transactionFilter.filter(masterData);
        transactionsTable.setItems(FXCollections.observableArrayList(filtered));
        calculateAndShowStats(filtered);
    }

    private BigDecimal parseBigDecimal(String text) {
        try {
            return text.isEmpty() ? null : new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void calculateAndShowStats(List<Transaction> transactions) {
        User user = userSession.getCurrentUser();
        Currency currency = user.getCurrency() != null ? user.getCurrency() : Currency.USD;
        String symbol = currency.getSymbol();

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            if (t.getStatus() == TransactionStatus.COMPLETED) {
                if (t.getType() == TransactionType.INCOME) income = income.add(t.getAmount());
                else expense = expense.add(t.getAmount());
            }
        }

        BigDecimal balance = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;
        totalBalanceLabel.setText(symbol + " " + balance);
        totalIncomeLabel.setText("+ " + symbol + " " + income);
        totalExpenseLabel.setText("- " + symbol + " " + expense);
    }

    @FXML
    private void handleAddTransaction() {
        try {
            String title = titleField.getText();
            String amountStr = amountField.getText();
            TransactionType type = typeBox.getValue();
            Category cat = categoryBox.getValue();
            LocalDate date = datePicker.getValue();
            TransactionStatus status = statusBox.getValue();
            Priority priority = priorityBox.getValue();
            LocalDate dueDate = dueDatePicker.getValue();
            String notes = notesArea.getText();

            if (title.isEmpty() || amountStr.isEmpty() || type == null || cat == null || date == null) {
                navigationService.showAlert("Error", "Please fill in all required fields");
                return;
            }

            BigDecimal amount = new BigDecimal(amountStr);
            User user = userSession.getCurrentUser();

            financeService.addTransaction(user, title, amount, type, cat, date, notes, dueDate, status, priority);

            titleField.clear();
            amountField.clear();
            notesArea.clear();
            dueDatePicker.setValue(null);
            loadData();

        } catch (NumberFormatException e) {
            navigationService.showAlert("Error", "Invalid amount format");
        } catch (Exception e) {
            navigationService.showAlert("Error", "Error saving transaction");
        }
    }

    private void handleDeleteTransaction(Transaction t) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Transaction");
        alert.setHeaderText("Delete '" + t.getTitle() + "'?");
        alert.setContentText("Funds will be restored if transaction was Completed.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            financeService.deleteTransaction(t.getId(), userSession.getCurrentUser());
            loadData();
        }
    }

    @FXML
    private void handleResetFilter() {
        filterStartDate.setValue(null);
        filterEndDate.setValue(null);
        filterStatus.setValue(null);
        filterCategory.setValue(null);
        filterMinAmount.clear();
        filterMaxAmount.clear();
        searchField.clear();
        applyFilters();
    }
}