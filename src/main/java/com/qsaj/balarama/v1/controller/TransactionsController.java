package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.*;
import com.qsaj.balarama.v1.model.enums.*;
import com.qsaj.balarama.v1.service.impl.CategoryServiceImpl;
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
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.function.UnaryOperator;

public class TransactionsController {

    @FXML private TextField titleField;
    @FXML private TextField amountField;
    @FXML private ComboBox<TransactionType> typeBox;
    @FXML private ComboBox<CategoryGroup> groupBox;
    @FXML private ComboBox<CategoryItem> categoryBox;
    @FXML private ComboBox<TransactionStatus> statusBox;
    @FXML private ComboBox<Priority> priorityBox;
    @FXML private DatePicker datePicker;
    @FXML private DatePicker dueDatePicker;
    @FXML private TextArea notesArea;
    @FXML private Button addTransactionButton;

    @FXML private Label totalBalanceLabel;
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpenseLabel;

    @FXML private DatePicker filterStartDate;
    @FXML private DatePicker filterEndDate;
    @FXML private ComboBox<TransactionStatus> filterStatus;
    @FXML private ComboBox<CategoryGroup> filterCategory;
    @FXML private ComboBox<CategoryItem> filterSubcategory;
    @FXML private TextField filterMinAmount;
    @FXML private TextField filterMaxAmount;
    @FXML private TextField searchField;

    @FXML private TableView<Transaction> transactionsTable;
    @FXML private TableColumn<Transaction, LocalDate> colDate;
    @FXML private TableColumn<Transaction, String> colTitle;
    @FXML private TableColumn<Transaction, TransactionType> colType;
    @FXML private TableColumn<Transaction, String> colCategory;
    @FXML private TableColumn<Transaction, String> colSubcategory;
    @FXML private TableColumn<Transaction, String> colStatus;
    @FXML private TableColumn<Transaction, LocalDate> colDueDate;
    @FXML private TableColumn<Transaction, String> colNotes;
    @FXML private TableColumn<Transaction, BigDecimal> colAmount;
    @FXML private TableColumn<Transaction, Void> colAction;

    private final UserSession userSession;
    private final FinanceService financeService;
    private final NavigationService navigationService;
    private final CategoryServiceImpl categoryService;
    private final ObservableList<Transaction> masterData = FXCollections.observableArrayList();
    private final TransactionFilter transactionFilter = new TransactionFilter();
    private Transaction transactionToEdit;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withResolverStyle(ResolverStyle.STRICT);

    @Inject
    public TransactionsController(UserSession userSession, FinanceService financeService,
                                  NavigationService navigationService, CategoryServiceImpl categoryService) {
        this.userSession = userSession;
        this.financeService = financeService;
        this.navigationService = navigationService;
        this.categoryService = categoryService;
    }

    @FXML
    public void initialize() {
        setupInputValidation();
        setupTableColumns();
        initializeComboBoxes();
        setupTypeChangeListener();
        setupGroupChangeListener();
        setupStatusLogic();

        SmartDateFormatter.apply(datePicker);
        SmartDateFormatter.apply(dueDatePicker);
        SmartDateFormatter.apply(filterStartDate);
        SmartDateFormatter.apply(filterEndDate);

        setupFilterListeners();

        resetForm();
        loadData();
    }

    private <T> void setupComboBoxDisplay(ComboBox<T> comboBox) {
        comboBox.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T object) {
                if (object == null) return "";
                if (object instanceof CategoryGroup) return ((CategoryGroup) object).getName();
                if (object instanceof CategoryItem) return ((CategoryItem) object).getName();
                return object.toString();
            }
            @Override
            public T fromString(String string) { return null; }
        });

        comboBox.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(comboBox.getPromptText());
                } else {
                    if (item instanceof CategoryGroup) setText(((CategoryGroup) item).getName());
                    else if (item instanceof CategoryItem) setText(((CategoryItem) item).getName());
                    else setText(item.toString());
                }
            }
        });

        comboBox.setCellFactory(lv -> new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item instanceof CategoryGroup) setText(((CategoryGroup) item).getName());
                    else if (item instanceof CategoryItem) setText(((CategoryItem) item).getName());
                    else setText(item.toString());
                }
            }
        });
    }

    private void setupFilterListeners() {
        filterSubcategory.setDisable(true);
        Tooltip filterSubTooltip = new Tooltip("Please select a Group first");
        filterSubcategory.setTooltip(filterSubTooltip);

        filterStartDate.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() == 10) {
                try {
                    LocalDate date = LocalDate.parse(newVal, DATE_FORMATTER);
                    filterStartDate.setValue(date);
                } catch (Exception ignored) {}
            }
            applyFilters();
        });
        filterEndDate.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() == 10) {
                try {
                    LocalDate date = LocalDate.parse(newVal, DATE_FORMATTER);
                    filterEndDate.setValue(date);
                } catch (Exception ignored) {}
            }
            applyFilters();
        });
        filterStartDate.valueProperty().addListener(o -> applyFilters());
        filterEndDate.valueProperty().addListener(o -> applyFilters());

        filterStatus.valueProperty().addListener(o -> applyFilters());

        filterCategory.valueProperty().addListener((o, oldV, newV) -> {
            if (newV == null) {
                filterSubcategory.getItems().clear();
                filterSubcategory.setValue(null);
                filterSubcategory.setDisable(true);
                filterSubcategory.setTooltip(filterSubTooltip);
            } else {
                List<CategoryItem> items = categoryService.getItemsForGroup(newV.getId());
                filterSubcategory.setItems(FXCollections.observableArrayList(items));
                filterSubcategory.setDisable(false);
                filterSubcategory.setTooltip(null);
            }
            applyFilters();
        });

        filterSubcategory.valueProperty().addListener(o -> applyFilters());
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
        CategoryItem selectedItem = categoryBox.getValue();
        if (selectedItem == null) {
            navigationService.showAlert("Error", "Please select a subcategory");
            return;
        }

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
                selectedItem, date, notesArea.getText(), dueDate,
                statusBox.getValue(), priorityBox.getValue());

        resetForm();
        loadData();
    }

    @FXML
    private void handleCancelEdit() {
        resetForm();
    }

    private void setupTypeChangeListener() {
        typeBox.valueProperty().addListener((o, oldV, newV) -> {
            if (newV == null) return;

            List<CategoryGroup> groups = categoryService.getGroupsForUserByType(userSession.getCurrentUser(), newV);
            groupBox.setItems(FXCollections.observableArrayList(groups));
            if (!groups.isEmpty()) {
                groupBox.getSelectionModel().selectFirst();
            } else {
                groupBox.getSelectionModel().clearSelection();
                categoryBox.getItems().clear();
                // Тільки якщо ми НЕ в режимі редагування показуємо алерт, щоб не спамило
                if (transactionToEdit == null) {
                    navigationService.showAlert("Info", "No category groups for this type. Please create some in Category Settings.");
                }
            }

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

    private void setupGroupChangeListener() {
        categoryBox.setDisable(true);
        Tooltip formSubTooltip = new Tooltip("Please select a Group first");
        categoryBox.setTooltip(formSubTooltip);

        groupBox.valueProperty().addListener((o, oldV, newV) -> {
            if (newV == null) {
                categoryBox.getItems().clear();
                categoryBox.setDisable(true);
                categoryBox.setTooltip(formSubTooltip);
                return;
            }
            List<CategoryItem> items = categoryService.getItemsForGroup(newV.getId());
            categoryBox.setItems(FXCollections.observableArrayList(items));
            categoryBox.setDisable(false);
            categoryBox.setTooltip(null);

            // Вибираємо перший елемент тільки якщо ми зараз нічого не редагуємо
            if (transactionToEdit == null) {
                if (!items.isEmpty()) {
                    categoryBox.getSelectionModel().selectFirst();
                } else {
                    categoryBox.getSelectionModel().clearSelection();
                }
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

    @FXML
    private void handleResetFilter() {
        filterStartDate.setValue(null);
        filterStartDate.getEditor().clear();
        filterEndDate.setValue(null);
        filterEndDate.getEditor().clear();

        filterMinAmount.clear();
        filterMaxAmount.clear();
        searchField.clear();

        filterStatus.setValue(null);
        filterCategory.setValue(null);
        filterSubcategory.getItems().clear();
        filterSubcategory.setValue(null);

        applyFilters();
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

        colType.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getType()));
        colType.setCellFactory(col -> new TableCell<Transaction, TransactionType>() {
            @Override
            protected void updateItem(TransactionType item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("amount-income", "amount-expense");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name());
                    if (item == TransactionType.INCOME) {
                        getStyleClass().add("amount-income");
                    } else {
                        getStyleClass().add("amount-expense");
                    }
                }
            }
        });

        colCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoryItem().getGroup().getName()));
        colSubcategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoryItem().getName()));

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

        setupComboBoxDisplay(groupBox);
        setupComboBoxDisplay(categoryBox);
        setupComboBoxDisplay(filterCategory);
        setupComboBoxDisplay(filterSubcategory);

        filterStatus.setButtonCell(new ListCell<TransactionStatus>() {
            @Override
            protected void updateItem(TransactionStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(filterStatus.getPromptText());
                else setText(item.name());
            }
        });

        User user = userSession.getCurrentUser();
        List<CategoryGroup> groups = categoryService.getGroupsForUser(user);
        filterCategory.setItems(FXCollections.observableArrayList(groups));
    }

    private void loadData() {
        masterData.setAll(financeService.getUserTransactions(userSession.getCurrentUser()));
        applyFilters();
    }

    private void applyFilters() {
        transactionFilter.setStartDate(SmartDateFormatter.getDate(filterStartDate));
        transactionFilter.setEndDate(SmartDateFormatter.getDate(filterEndDate));
        transactionFilter.setStatus(filterStatus.getValue());
        transactionFilter.setCategoryGroup(filterCategory.getValue());
        transactionFilter.setCategoryItem(filterSubcategory.getValue());
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

    // --- ВИПРАВЛЕНО: Заповнення групи та підкатегорії під час редагування за ID ---
    public void startEdit(Transaction t) {
        if (t == null) return;
        this.transactionToEdit = t;
        titleField.setText(t.getTitle());
        amountField.setText(t.getAmount() != null ? t.getAmount().toString() : "");

        // 1. Встановлюємо тип. Спрацьовує лістенер і заповнює список groupBox
        typeBox.setValue(t.getType());

        // 2. Шукаємо відповідну групу за ID у новому списку і вибираємо її
        CategoryGroup targetGroup = groupBox.getItems().stream()
                .filter(g -> g.getId().equals(t.getCategoryItem().getGroup().getId()))
                .findFirst()
                .orElse(null);
        groupBox.setValue(targetGroup);

        // 3. Відбувся вибір групи. Спрацьовує лістенер і заповнює список categoryBox.
        // Тепер знаходимо потрібну підкатегорію за ID.
        CategoryItem targetItem = categoryBox.getItems().stream()
                .filter(item -> item.getId().equals(t.getCategoryItem().getId()))
                .findFirst()
                .orElse(null);
        categoryBox.setValue(targetItem);

        statusBox.setValue(t.getStatus());
        priorityBox.setValue(t.getPriority());
        datePicker.setValue(t.getDate());
        dueDatePicker.setValue(t.getDueDate());
        notesArea.setText(t.getNotes() != null ? t.getNotes() : "");

        addTransactionButton.setText("Update Transaction");
        if (!addTransactionButton.getStyleClass().contains("button-state-edit"))
            addTransactionButton.getStyleClass().add("button-state-edit");
    }

    public void deleteTransaction(Transaction t) {
        if (navigationService.showConfirmation("Delete Transaction", "Delete '" + t.getTitle() + "'?", "Funds will be restored.")) {
            financeService.deleteTransaction(t.getId(), userSession.getCurrentUser());
            loadData();
        }
    }
}