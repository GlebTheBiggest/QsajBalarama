package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.Transaction;
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
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

public class TransactionsController {

    @FXML
    private TextField titleField;
    @FXML
    private TextField amountField;
    @FXML
    private ComboBox<TransactionType> typeBox;
    @FXML
    private ComboBox<Category> categoryBox;
    @FXML
    private ComboBox<TransactionStatus> statusBox;
    @FXML
    private ComboBox<Priority> priorityBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private DatePicker dueDatePicker;
    @FXML
    private TextArea notesArea;
    @FXML
    private Button addTransactionButton;

    @FXML
    private Label totalBalanceLabel;
    @FXML
    private Label totalIncomeLabel;
    @FXML
    private Label totalExpenseLabel;

    @FXML
    private DatePicker filterStartDate;
    @FXML
    private DatePicker filterEndDate;
    @FXML
    private ComboBox<TransactionStatus> filterStatus;
    @FXML
    private ComboBox<Category> filterCategory;
    @FXML
    private TextField filterMinAmount;
    @FXML
    private TextField filterMaxAmount;
    @FXML
    private TextField searchField;

    @FXML
    private TableView<Transaction> transactionsTable;
    @FXML
    private TableColumn<Transaction, LocalDate> colDate;
    @FXML
    private TableColumn<Transaction, String> colTitle;
    @FXML
    private TableColumn<Transaction, String> colCategory;
    @FXML
    private TableColumn<Transaction, String> colStatus;
    @FXML
    private TableColumn<Transaction, LocalDate> colDueDate;
    @FXML
    private TableColumn<Transaction, String> colNotes;
    @FXML
    private TableColumn<Transaction, BigDecimal> colAmount;
    @FXML
    private TableColumn<Transaction, Void> colAction;

    private final UserSession userSession;
    private final FinanceService financeService;
    private final NavigationService navigationService;
    private final ObservableList<Transaction> masterData = FXCollections.observableArrayList();
    private final TransactionFilter transactionFilter = new TransactionFilter();
    private Transaction transactionToEdit;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withResolverStyle(ResolverStyle.STRICT);

    @Inject
    public TransactionsController(UserSession userSession, FinanceService financeService, NavigationService navigationService) {
        this.userSession = userSession;
        this.financeService = financeService;
        this.navigationService = navigationService;
    }

    @FXML
    public void initialize() {
        setupInputValidation();
        setupTableColumns();
        initializeComboBoxes();
        setupTypeChangeListener();
        setupStatusLogic();

        SmartDateFormatter.apply(datePicker);
        SmartDateFormatter.apply(dueDatePicker);
        SmartDateFormatter.apply(filterStartDate);
        SmartDateFormatter.apply(filterEndDate);

        // Спеціальні слухачі для фільтрів дати
        setupFilterDateListeners();

        resetForm();
        loadData();
    }

    private void setupFilterDateListeners() {
        // Слухачі для ручного введення дати (коли текст досягає 10 символів)
        filterStartDate.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() == 10) {
                try {
                    LocalDate date = LocalDate.parse(newVal, DATE_FORMATTER);
                    filterStartDate.setValue(date);
                } catch (Exception ignored) {
                }
            }
            applyFilters();
        });
        filterEndDate.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() == 10) {
                try {
                    LocalDate date = LocalDate.parse(newVal, DATE_FORMATTER);
                    filterEndDate.setValue(date);
                } catch (Exception ignored) {
                }
            }
            applyFilters();
        });

        // При виборі дати з календаря
        filterStartDate.valueProperty().addListener(o -> applyFilters());
        filterEndDate.valueProperty().addListener(o -> applyFilters());

        // Інші фільтри
        filterStatus.valueProperty().addListener(o -> applyFilters());
        filterCategory.valueProperty().addListener(o -> applyFilters());
        searchField.textProperty().addListener(o -> applyFilters());
        filterMinAmount.textProperty().addListener(o -> applyFilters());
        filterMaxAmount.textProperty().addListener(o -> applyFilters());
    }

    @FXML
    private void handleAddTransaction() {
        if (!SmartDateFormatter.isValid(datePicker)) {
            datePicker.getStyleClass().add("field-error");
            navigationService.showAlert("Invalid Date", "Use DD.MM.YYYY");
            return;
        }

        if (titleField.getText().isBlank() || amountField.getText().isBlank() || typeBox.getValue() == null) {
            navigationService.showAlert("Validation Error", "Title, Amount and Type are required");
            return;
        }

        if (statusBox.getValue() == TransactionStatus.PENDING && !SmartDateFormatter.isValid(dueDatePicker)) {
            dueDatePicker.getStyleClass().add("field-error");
            navigationService.showAlert("Validation Error", "Due Date required for Pending");
            return;
        }

        BigDecimal amount = new BigDecimal(amountField.getText());

        if (typeBox.getValue() == TransactionType.EXPENSE) {
            BigDecimal currentBalance = userSession.getCurrentUser().getSavedAmount();
            if (currentBalance == null) currentBalance = BigDecimal.ZERO;

            if (currentBalance.subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
                boolean confirm = navigationService.showConfirmation(
                        "Negative Balance Warning",
                        "Insufficient funds.",
                        "Current: " + currentBalance + "\nAfter transaction: " + currentBalance.subtract(amount) + "\nProceed anyway?"
                );
                if (!confirm) return;
            }
        }

        LocalDate date = SmartDateFormatter.getDate(datePicker);
        LocalDate dueDate = SmartDateFormatter.getDate(dueDatePicker);
        var user = userSession.getCurrentUser();

        if (transactionToEdit != null) {
            financeService.deleteTransaction(transactionToEdit.getId(), user);
        }

        financeService.addTransaction(user, titleField.getText(), amount, typeBox.getValue(),
                categoryBox.getValue(), date, notesArea.getText(), dueDate,
                statusBox.getValue(), priorityBox.getValue());

        resetForm();
        loadData();
    }

    private void setupTypeChangeListener() {
        typeBox.valueProperty().addListener((o, oldV, newV) -> {
            if (newV == null) return;

            categoryBox.getItems().setAll(Arrays.stream(Category.values()).filter(c -> c.getType() == newV).toList());
            categoryBox.getSelectionModel().selectFirst();

            boolean income = newV == TransactionType.INCOME;
            dueDatePicker.setDisable(income);
            statusBox.setDisable(income);
            priorityBox.setDisable(income);

            if (income) {
                dueDatePicker.setValue(null);
                statusBox.setValue(TransactionStatus.COMPLETED);
                priorityBox.setValue(Priority.STANDARD);
            } else {
                statusBox.setDisable(false);
                priorityBox.setDisable(false);
                dueDatePicker.setDisable(statusBox.getValue() != TransactionStatus.PENDING);
            }
        });
    }

    private void resetForm() {
        transactionToEdit = null;
        titleField.clear();
        amountField.clear();
        notesArea.clear();
        dueDatePicker.setValue(null);
        datePicker.setValue(LocalDate.now());

        typeBox.setValue(TransactionType.EXPENSE);
        statusBox.setValue(TransactionStatus.COMPLETED);
        priorityBox.setValue(Priority.STANDARD);

        addTransactionButton.setText("Add Transaction");
        addTransactionButton.getStyleClass().remove("button-state-edit");

        datePicker.getStyleClass().remove("field-error");
        dueDatePicker.getStyleClass().remove("field-error");
    }

    private <T> void setComboPrompt(ComboBox<T> combo, String prompt) {
        combo.setPromptText(prompt);
        combo.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(prompt);
                } else {
                    setText(item.toString());
                }
            }
        });
        combo.setCellFactory(lv -> new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.toString());
            }
        });
    }

    @FXML
    private void handleResetFilter() {
        filterStartDate.setValue(null);
        filterStartDate.getEditor().clear();
        filterEndDate.setValue(null);
        filterEndDate.getEditor().clear();

        filterMinAmount.clear();
        filterMaxAmount.clear();
        searchField.clear();

        resetCombo(filterStatus, "Status");
        resetCombo(filterCategory, "Category");

        applyFilters();
    }

    private <T> void resetCombo(ComboBox<T> combo, String prompt) {
        combo.setValue(null);
        setComboPrompt(combo, prompt);
    }

    private void setupStatusLogic() {
        statusBox.valueProperty().addListener((o, oldV, newV) -> dueDatePicker.setDisable(newV != TransactionStatus.PENDING));
    }

    private void setupInputValidation() {
        UnaryOperator<TextFormatter.Change> moneyFilter = c -> c.getControlNewText().matches("-?([0-9]*\\.?[0-9]*)?") ? c : null;
        amountField.setTextFormatter(new TextFormatter<>(moneyFilter));
        filterMinAmount.setTextFormatter(new TextFormatter<>(moneyFilter));
        filterMaxAmount.setTextFormatter(new TextFormatter<>(moneyFilter));
    }

    private void setupTableColumns() {
        String symbol = userSession.getCurrentUser().getCurrency() != null ? userSession.getCurrentUser().getCurrency().getSymbol() : "$";
        colDate.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getDate()));
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        colTitle.setCellFactory(c -> new TransactionTitleCell());
        colCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory().name()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        colStatus.setCellFactory(c -> new StatusTableCell());
        colDueDate.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getDueDate()));
        colNotes.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNotes()));
        colNotes.setCellFactory(c -> new NotesTableCell());
        colAmount.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getAmount()));
        colAmount.setCellFactory(c -> new AmountTableCell(symbol));
        colAction.setCellFactory(c -> new ActionTableCell(this));
    }

    private void initializeComboBoxes() {
        typeBox.getItems().setAll(TransactionType.values());
        statusBox.getItems().setAll(TransactionStatus.values());
        priorityBox.getItems().setAll(Priority.values());
        filterStatus.getItems().setAll(TransactionStatus.values());
        filterCategory.getItems().setAll(Category.values());
        setComboPrompt(statusBox, "Status");
        setComboPrompt(categoryBox, "Category");
        setComboPrompt(filterStatus, "Status");
        setComboPrompt(filterCategory, "Category");
    }

    private void loadData() {
        masterData.setAll(financeService.getUserTransactions(userSession.getCurrentUser()));
        applyFilters();
    }

    private void applyFilters() {
        transactionFilter.setStartDate(SmartDateFormatter.getDate(filterStartDate));
        transactionFilter.setEndDate(SmartDateFormatter.getDate(filterEndDate));
        transactionFilter.setStatus(filterStatus.getValue());
        transactionFilter.setCategory(filterCategory.getValue());
        transactionFilter.setSearchText(searchField.getText());
        transactionFilter.setMinAmount(parseBigDecimal(filterMinAmount.getText()));
        transactionFilter.setMaxAmount(parseBigDecimal(filterMaxAmount.getText()));

        List<Transaction> filtered = transactionFilter.filter(masterData);
        transactionsTable.setItems(FXCollections.observableArrayList(filtered));
        calculateAndShowStats(masterData);
    }

    private BigDecimal parseBigDecimal(String text) {
        try {
            return text == null || text.isBlank() ? null : new BigDecimal(text);
        } catch (Exception e) {
            return null;
        }
    }

    private void calculateAndShowStats(List<Transaction> list) {
        BigDecimal income = BigDecimal.ZERO, expense = BigDecimal.ZERO;
        for (Transaction t : list) {
            if (t.getType() == TransactionType.INCOME) income = income.add(t.getAmount());
            else expense = expense.add(t.getAmount());
        }
        var u = userSession.getCurrentUser();
        String sym = u.getCurrency() != null ? u.getCurrency().getSymbol() : "$";
        totalBalanceLabel.setText(sym + " " + (u.getSavedAmount() != null ? u.getSavedAmount() : BigDecimal.ZERO));
        totalIncomeLabel.setText("+ " + sym + " " + income);
        totalExpenseLabel.setText("- " + sym + " " + expense);
    }

    public void startEdit(Transaction t) {
        if (t == null) return;
        this.transactionToEdit = t;
        titleField.setText(t.getTitle());
        amountField.setText(t.getAmount() != null ? t.getAmount().toString() : "");
        typeBox.setValue(t.getType());
        categoryBox.setValue(t.getCategory());
        statusBox.setValue(t.getStatus());
        priorityBox.setValue(t.getPriority());
        datePicker.setValue(t.getDate());
        dueDatePicker.setValue(t.getDueDate());
        notesArea.setText(t.getNotes() != null ? t.getNotes() : "");
        if (addTransactionButton != null) {
            addTransactionButton.setText("Update Transaction");
            if (!addTransactionButton.getStyleClass().contains("button-state-edit"))
                addTransactionButton.getStyleClass().add("button-state-edit");
        }
    }

    public void deleteTransaction(Transaction t) {
        if (navigationService.showConfirmation("Delete Transaction", "Delete '" + t.getTitle() + "'?", "Funds will be restored.")) {
            financeService.deleteTransaction(t.getId(), userSession.getCurrentUser());
            loadData();
        }
    }
}