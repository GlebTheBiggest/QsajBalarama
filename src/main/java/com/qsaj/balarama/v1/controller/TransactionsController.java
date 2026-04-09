package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.*;
import com.qsaj.balarama.v1.model.enums.*;
import com.qsaj.balarama.v1.repository.TransactionRepository;
import com.qsaj.balarama.v1.service.BudgetService;
import com.qsaj.balarama.v1.service.EventService;
import com.qsaj.balarama.v1.service.SavingsService;
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
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

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
    @FXML private ComboBox<TransactionType> filterType;
    @FXML private ComboBox<CategoryGroup> filterCategory;
    @FXML private ComboBox<CategoryItem> filterSubcategory;
    @FXML private TextField filterMinAmount;
    @FXML private TextField filterMaxAmount;
    @FXML private TextField searchField;
    @FXML private CheckBox onlyCurrentPeriodCheckBox;

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

    @FXML private VBox emptyStateOverlay;

    private final UserSession userSession;
    private final FinanceService financeService;
    private final NavigationService navigationService;
    private final CategoryServiceImpl categoryService;
    private final BudgetService budgetService;
    private final SavingsService savingsService;
    private final TransactionRepository transactionRepository;
    private final EventService eventService;
    private final ObservableList<Transaction> masterData = FXCollections.observableArrayList();
    private final TransactionFilter transactionFilter = new TransactionFilter();
    private Transaction transactionToEdit;
    private boolean isOnlyCurrentPeriod = true;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withResolverStyle(ResolverStyle.STRICT);

    @Inject
    public TransactionsController(UserSession userSession, FinanceService financeService,
                                  NavigationService navigationService, CategoryServiceImpl categoryService,
                                  BudgetService budgetService, SavingsService savingsService,
                                  TransactionRepository transactionRepository, EventService eventService) {
        this.userSession = userSession;
        this.financeService = financeService;
        this.navigationService = navigationService;
        this.categoryService = categoryService;
        this.budgetService = budgetService;
        this.savingsService = savingsService;
        this.transactionRepository = transactionRepository;
        this.eventService = eventService;
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

        onlyCurrentPeriodCheckBox.selectedProperty().addListener((obs, old, newVal) -> {
            isOnlyCurrentPeriod = newVal;
            loadData();
        });
        updateOnlyCurrentPeriodState();

        resetForm();
        loadData();
    }

    private void updateOnlyCurrentPeriodState() {
        User user = userSession.getCurrentUser();
        boolean isPeriodic = user.getAppMode() == AppMode.PERIODIC;
        onlyCurrentPeriodCheckBox.setDisable(!isPeriodic);
        if (!isPeriodic) {
            onlyCurrentPeriodCheckBox.setSelected(false);
            isOnlyCurrentPeriod = false;
        } else {
            onlyCurrentPeriodCheckBox.setSelected(isOnlyCurrentPeriod);
        }
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
        filterType.valueProperty().addListener(o -> applyFilters());

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

        if (selectedItem.getGroup().getName().equalsIgnoreCase("System")) {
            navigationService.showAlert("Error", "System category cannot be used for manual transactions.");
            return;
        }

        var user = userSession.getCurrentUser();

        if (user.getAppMode() == AppMode.PERIODIC && budgetService.getActivePeriod(user) == null) {
            navigationService.showAlert("No Active Period",
                    "Cannot add transaction. Please start a budget period first (Budgets page).");
            return;
        }

        if (typeBox.getValue() == TransactionType.INCOME && selectedItem.getGroup().getName().equalsIgnoreCase("Income")) {
            SavingsGoal goal = savingsService.getUserGoals(user).stream()
                    .filter(g -> g.getName().equalsIgnoreCase(selectedItem.getName()))
                    .findFirst().orElse(null);

            if (goal != null) {
                BigDecimal available = goal.getCurrentAmount();
                if (transactionToEdit != null && transactionToEdit.getType() == TransactionType.INCOME
                        && selectedItem.getName().equals(transactionToEdit.getSnapshotItemName())) {
                    available = available.add(transactionToEdit.getAmount());
                }
                if (available.compareTo(amount) < 0) {
                    String symbol = user.getCurrency() != null ? user.getCurrency().getSymbol() : "$";
                    navigationService.showAlert("Insufficient Funds",
                            "Not enough money in envelope '" + goal.getName() + "'.\nAvailable: " + symbol + " " + available);
                    return;
                }
            }
        }

        if (typeBox.getValue() == TransactionType.EXPENSE) {
            BigDecimal currentBalance = user.getSavedAmount() != null ? user.getSavedAmount() : BigDecimal.ZERO;
            if (transactionToEdit != null && transactionToEdit.getType() == TransactionType.EXPENSE) {
                currentBalance = currentBalance.add(transactionToEdit.getAmount());
            }
            if (currentBalance.subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
                boolean confirm = navigationService.showConfirmation(
                        "Negative Balance Warning",
                        "Insufficient funds.",
                        "Current available: " + currentBalance + "\nAfter transaction: " + currentBalance.subtract(amount) + "\nProceed anyway?"
                );
                if (!confirm) return;
            }
        }

        LocalDate date = SmartDateFormatter.getDate(datePicker);
        LocalDate dueDate = SmartDateFormatter.getDate(dueDatePicker);

        if (user.getAppMode() == AppMode.PERIODIC && typeBox.getValue() == TransactionType.EXPENSE &&
                !selectedItem.getGroup().getName().equalsIgnoreCase("Savings") &&
                !selectedItem.getGroup().getName().equalsIgnoreCase("System")) {
            BudgetPeriod activePeriod = budgetService.getActivePeriod(user);
            if (activePeriod != null) {
                CategoryBudget budget = budgetService.getBudgetsForPeriod(activePeriod).stream()
                        .filter(b -> b.getCategoryGroup().getId().equals(selectedItem.getGroup().getId()))
                        .findFirst().orElse(null);
                if (budget != null) {
                    BigDecimal newSpent = budget.getSpentAmount().add(amount);
                    if (newSpent.compareTo(budget.getAllocatedAmount()) > 0) {
                        boolean increased = showIncreaseLimitDialog(selectedItem.getGroup(), budget.getAllocatedAmount(), newSpent);
                        if (!increased) return;
                    }
                }
            }
        }

        if (transactionToEdit != null) {
            financeService.deleteTransaction(transactionToEdit.getId(), user);
        }

        try {
            financeService.addTransaction(user, titleField.getText(), amount, typeBox.getValue(),
                    selectedItem, date, notesArea.getText(), dueDate,
                    statusBox.getValue(), priorityBox.getValue());
            resetForm();
            loadData();
        } catch (IllegalArgumentException ex) {
            loadData();
            navigationService.showAlert("Error", ex.getMessage());
        }
    }

    private boolean showIncreaseLimitDialog(CategoryGroup group, BigDecimal currentLimit, BigDecimal required) {
        TextInputDialog dialog = new TextInputDialog(currentLimit.toString());
        dialog.setTitle("Increase Budget Limit");
        dialog.setHeaderText("Current limit for " + group.getName() + " is " + currentLimit +
                ". You need at least " + required + " to add this transaction.");
        dialog.setContentText("New limit:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                BigDecimal newLimit = new BigDecimal(result.get());
                if (newLimit.compareTo(required) >= 0) {
                    BudgetPeriod activePeriod = budgetService.getActivePeriod(userSession.getCurrentUser());
                    if (activePeriod != null) {
                        budgetService.setCategoryBudget(activePeriod, group, newLimit);
                        return true;
                    }
                } else {
                    navigationService.showAlert("Invalid Limit", "New limit must be at least " + required);
                }
            } catch (NumberFormatException e) {
                navigationService.showAlert("Error", "Invalid number format");
            }
        }
        return false;
    }

    @FXML
    private void handleCancelEdit() {
        resetForm();
    }

    private void checkEmptyState() {
        User user = userSession.getCurrentUser();
        List<CategoryGroup> visibleGroups = categoryService.getUserVisibleGroups(user);

        boolean hasAnySubcategory = false;
        for (CategoryGroup group : visibleGroups) {
            if (!categoryService.getItemsForGroup(group.getId()).isEmpty()) {
                hasAnySubcategory = true;
                break;
            }
        }

        if (!hasAnySubcategory && masterData.isEmpty()) {
            emptyStateOverlay.setVisible(true);
            emptyStateOverlay.toFront();
        } else {
            emptyStateOverlay.setVisible(false);
            emptyStateOverlay.toBack();
        }
    }

    private void setupTypeChangeListener() {
        typeBox.valueProperty().addListener((o, oldV, newV) -> {
            if (newV == null) return;

            List<CategoryGroup> groups = categoryService.getGroupsForUserByType(userSession.getCurrentUser(), newV);
            groupBox.setItems(FXCollections.observableArrayList(groups));

            boolean hasGroups = !groups.isEmpty();

            titleField.setDisable(!hasGroups);
            amountField.setDisable(!hasGroups);
            groupBox.setDisable(!hasGroups);
            datePicker.setDisable(!hasGroups);
            notesArea.setDisable(!hasGroups);

            if (!hasGroups) {
                groupBox.getSelectionModel().clearSelection();
                categoryBox.getItems().clear();
                categoryBox.setDisable(true);
                addTransactionButton.setText("No Categories Found");
                addTransactionButton.setDisable(true);
                statusBox.setDisable(true);
                priorityBox.setDisable(true);
                dueDatePicker.setDisable(true);
            } else {
                if (transactionToEdit == null) {
                    groupBox.getSelectionModel().selectFirst();
                }
                addTransactionButton.setText(transactionToEdit == null ? "Add Transaction" : "Update Transaction");
                boolean income = newV == TransactionType.INCOME;
                statusBox.setDisable(income);
                priorityBox.setDisable(income);
                dueDatePicker.setDisable(income || statusBox.getValue() != TransactionStatus.PENDING);
                if (income) {
                    dueDatePicker.setValue(null);
                    statusBox.setValue(TransactionStatus.COMPLETED);
                    priorityBox.setValue(Priority.STANDARD);
                }
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
                addTransactionButton.setDisable(true);
                return;
            }
            List<CategoryItem> items = categoryService.getItemsForGroup(newV.getId());
            categoryBox.setItems(FXCollections.observableArrayList(items));

            boolean hasItems = !items.isEmpty();
            categoryBox.setDisable(!hasItems);
            addTransactionButton.setDisable(!hasItems);

            if (hasItems) {
                categoryBox.setTooltip(null);
                if (transactionToEdit == null) {
                    categoryBox.getSelectionModel().selectFirst();
                }
            } else {
                Tooltip emptyTooltip = new Tooltip("No subcategories available");
                categoryBox.setTooltip(emptyTooltip);
                categoryBox.getSelectionModel().clearSelection();
                addTransactionButton.setText("Select Subcategory");
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
        filterType.setValue(null);
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
                getStyleClass().removeAll("amount-income", "amount-expense", "amount-transfer");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name());
                    switch (item) {
                        case INCOME -> getStyleClass().add("amount-income");
                        case EXPENSE -> getStyleClass().add("amount-expense");
                        case TRANSFER -> getStyleClass().add("amount-transfer");
                    }
                }
            }
        });

        colCategory.setCellValueFactory(d -> {
            String name = d.getValue().getSnapshotGroupName();
            return new SimpleStringProperty(name != null ? name : "Unknown");
        });

        colSubcategory.setCellValueFactory(d -> {
            String name = d.getValue().getSnapshotItemName();
            return new SimpleStringProperty(name != null ? name : "Unknown");
        });

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
        typeBox.getItems().setAll(TransactionType.INCOME, TransactionType.EXPENSE);
        statusBox.getItems().setAll(TransactionStatus.values());
        priorityBox.getItems().setAll(Priority.values());

        filterStatus.getItems().setAll(TransactionStatus.values());
        filterType.getItems().setAll(null, TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.TRANSFER);
        filterType.setCellFactory(lv -> new ListCell<TransactionType>() {
            @Override protected void updateItem(TransactionType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All Types" : item.name());
            }
        });
        filterType.setButtonCell(new ListCell<TransactionType>() {
            @Override protected void updateItem(TransactionType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All Types" : item.name());
            }
        });

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
        List<CategoryGroup> groups = categoryService.getUserVisibleGroups(user);
        filterCategory.setItems(FXCollections.observableArrayList(groups));
    }

    private void loadData() {
        User user = userSession.getCurrentUser();
        updateOnlyCurrentPeriodState();

        if (user.getAppMode() == AppMode.PERIODIC && onlyCurrentPeriodCheckBox.isSelected()) {
            BudgetPeriod active = budgetService.getActivePeriod(user);
            if (active != null) {
                masterData.setAll(transactionRepository.findAllByBudgetPeriod(active));
            } else {
                masterData.clear();
            }
        } else {
            masterData.setAll(financeService.getUserTransactions(user));
        }

        applyFilters();
        checkEmptyState();
    }

    private void applyFilters() {
        transactionFilter.setStartDate(SmartDateFormatter.getDate(filterStartDate));
        transactionFilter.setEndDate(SmartDateFormatter.getDate(filterEndDate));
        transactionFilter.setStatus(filterStatus.getValue());
        transactionFilter.setType(filterType.getValue());
        transactionFilter.setCategoryGroup(filterCategory.getValue());
        transactionFilter.setCategoryItem(filterSubcategory.getValue());
        transactionFilter.setSearchText(searchField.getText());
        transactionFilter.setMinAmount(parseBigDecimal(filterMinAmount.getText()));
        transactionFilter.setMaxAmount(parseBigDecimal(filterMaxAmount.getText()));

        List<Transaction> filtered = transactionFilter.filter(masterData);
        transactionsTable.setItems(FXCollections.observableArrayList(filtered));
        calculateAndShowStats();
    }

    private BigDecimal parseBigDecimal(String text) {
        try {
            return text == null || text.isBlank() ? null : new BigDecimal(text);
        } catch (Exception e) {
            return null;
        }
    }

    private void calculateAndShowStats() {
        BigDecimal income = BigDecimal.ZERO, expense = BigDecimal.ZERO;
        for (Transaction t : masterData) {
            if (t.getType() == TransactionType.INCOME) income = income.add(t.getAmount());
            else if (t.getType() == TransactionType.EXPENSE) expense = expense.add(t.getAmount());
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

        if (t.getCategoryItem() != null && t.getCategoryItem().getGroup() != null) {
            CategoryGroup targetGroup = groupBox.getItems().stream()
                    .filter(g -> g.getId().equals(t.getCategoryItem().getGroup().getId()))
                    .findFirst()
                    .orElse(null);
            groupBox.setValue(targetGroup);

            CategoryItem targetItem = categoryBox.getItems().stream()
                    .filter(item -> item.getId().equals(t.getCategoryItem().getId()))
                    .findFirst()
                    .orElse(null);
            categoryBox.setValue(targetItem);
        } else {
            groupBox.getSelectionModel().clearSelection();
            categoryBox.getSelectionModel().clearSelection();
        }

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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Transaction");
        alert.setHeaderText("Delete '" + t.getTitle() + "'?");
        alert.setContentText("Do you want to refund the amount to your balance?");
        ButtonType refundBtn = new ButtonType("Delete with Refund");
        ButtonType noRefundBtn = new ButtonType("Delete without Refund (keep history)");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(refundBtn, noRefundBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            User user = userSession.getCurrentUser();
            if (result.get() == refundBtn) {
                financeService.deleteTransaction(t.getId(), user);
                eventService.logEvent(user, EventType.TRANSACTION_DELETED,
                        "Deleted transaction with refund: " + t.getTitle());
                navigationService.showAlert("Success", "Transaction deleted and funds refunded.");
            } else if (result.get() == noRefundBtn) {
                financeService.deleteTransactionWithoutRefund(t.getId(), user);
                eventService.logEvent(user, EventType.TRANSACTION_DELETED,
                        "Deleted transaction without refund: " + t.getTitle());
                navigationService.showAlert("Success", "Transaction removed from history (balance unchanged).");
            } else {
                return;
            }
            loadData();
        }
    }
}