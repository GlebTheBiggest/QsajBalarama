package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.*;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.service.BudgetService;
import com.qsaj.balarama.v1.service.NavigationService;
import com.qsaj.balarama.v1.service.SavingsService;
import com.qsaj.balarama.v1.service.impl.CategoryServiceImpl;
import com.qsaj.balarama.v1.session.UserSession;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

public class CategorySettingsController {

    @FXML private TextField groupNameField;
    @FXML private ComboBox<TransactionType> groupTypeCombo;
    @FXML private ColorPicker groupColorPicker;
    @FXML private Button saveGroupBtn;
    @FXML private VBox subcategoriesSection;

    @FXML private TextField itemNameField;
    @FXML private Button addItemBtn;
    @FXML private ListView<CategoryItem> itemsListView;

    @FXML private TextField filterNameField;
    @FXML private ComboBox<TransactionType> filterTypeCombo;
    @FXML private ColorPicker filterColorPicker;

    @FXML private TableView<CategoryGroup> groupsTable;
    @FXML private TableColumn<CategoryGroup, String> colName;
    @FXML private TableColumn<CategoryGroup, TransactionType> colType;
    @FXML private TableColumn<CategoryGroup, String> colColor;
    @FXML private TableColumn<CategoryGroup, Void> colActions;

    private final UserSession userSession;
    private final CategoryServiceImpl categoryService;
    private final NavigationService navigationService;
    private final SavingsService savingsService;
    private final BudgetService budgetService;

    private CategoryGroup selectedGroup;
    private CategoryItem editingItem;
    private final ObservableList<CategoryGroup> masterGroups = FXCollections.observableArrayList();
    private final FilteredList<CategoryGroup> filteredGroups = new FilteredList<>(masterGroups, p -> true);
    private final ObservableList<CategoryItem> items = FXCollections.observableArrayList();

    private static final String SYSTEM_GROUP_INCOME = "Income";
    private static final String SYSTEM_GROUP_SAVINGS = "Savings";
    private static final String SYSTEM_GROUP_SYSTEM = "System";

    @Inject
    public CategorySettingsController(UserSession userSession,
                                      CategoryServiceImpl categoryService,
                                      NavigationService navigationService,
                                      SavingsService savingsService,
                                      BudgetService budgetService) {
        this.userSession = userSession;
        this.categoryService = categoryService;
        this.navigationService = navigationService;
        this.savingsService = savingsService;
        this.budgetService = budgetService;
    }

    @FXML
    public void initialize() {
        setupTypeComboBoxes();
        setupTable();
        setupFilters();
        loadGroups();
        setupItemList();
        clearGroupForm();

        itemsListView.setOnMouseClicked(e -> {
            if (itemsListView.getSelectionModel().isEmpty()) {
                itemsListView.getSelectionModel().clearSelection();
            }
        });

        filterColorPicker.setValue(null);
    }

    private boolean isSystemGroup(CategoryGroup group) {
        return group != null &&
                (SYSTEM_GROUP_INCOME.equalsIgnoreCase(group.getName()) ||
                        SYSTEM_GROUP_SAVINGS.equalsIgnoreCase(group.getName()) ||
                        SYSTEM_GROUP_SYSTEM.equalsIgnoreCase(group.getName()));
    }

    private boolean isSavingsGoalItem(CategoryItem item) {
        if (item == null || item.getGroup() == null) return false;
        User user = userSession.getCurrentUser();
        Set<String> goalNames = savingsService.getUserGoals(user).stream()
                .map(SavingsGoal::getName)
                .collect(Collectors.toSet());
        return goalNames.contains(item.getName());
    }

    private boolean isProtectedItem(CategoryItem item) {
        if (item == null) return false;
        CategoryGroup group = item.getGroup();
        if (group == null) return false;

        if (SYSTEM_GROUP_SAVINGS.equalsIgnoreCase(group.getName())) {
            return true;
        }
        if (SYSTEM_GROUP_INCOME.equalsIgnoreCase(group.getName())) {
            return isSavingsGoalItem(item);
        }
        return false;
    }

    private void setupTypeComboBoxes() {
        groupTypeCombo.setItems(FXCollections.observableArrayList(TransactionType.INCOME, TransactionType.EXPENSE));
        groupTypeCombo.getSelectionModel().selectFirst();

        filterTypeCombo.setItems(FXCollections.observableArrayList(TransactionType.values()));
        filterTypeCombo.getItems().add(0, null);
        filterTypeCombo.setCellFactory(lv -> new ListCell<TransactionType>() {
            @Override
            protected void updateItem(TransactionType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All" : item.name());
            }
        });
        filterTypeCombo.setButtonCell(new ListCell<TransactionType>() {
            @Override
            protected void updateItem(TransactionType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All" : item.name());
            }
        });
        filterTypeCombo.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void setupTable() {
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        colType.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getType()));
        colType.setCellFactory(col -> new TableCell<CategoryGroup, TransactionType>() {
            @Override
            protected void updateItem(TransactionType item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("amount-income", "amount-expense");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name());
                    getStyleClass().add(item == TransactionType.INCOME ? "amount-income" : "amount-expense");
                }
            }
        });

        colColor.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getColor()));
        colColor.setCellFactory(col -> new TableCell<CategoryGroup, String>() {
            private final Region swatch = new Region();
            {
                swatch.setMinSize(20, 20);
                swatch.setMaxSize(20, 20);
                swatch.setStyle("-fx-background-radius: 4; -fx-border-radius: 4; -fx-border-color: #cccccc;");
                setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(String colorHex, boolean empty) {
                super.updateItem(colorHex, empty);
                if (empty || colorHex == null) {
                    setGraphic(null);
                } else {
                    swatch.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 4; -fx-border-radius: 4; -fx-border-color: #cccccc;");
                    setGraphic(swatch);
                }
            }
        });

        colActions.setCellFactory(new Callback<>() {
            @Override
            public TableCell<CategoryGroup, Void> call(TableColumn<CategoryGroup, Void> param) {
                return new TableCell<>() {
                    private final Button editBtn = new Button("Edit");
                    private final Button deleteBtn = new Button("Delete");
                    private final HBox pane = new HBox(5, editBtn, deleteBtn);
                    private final Tooltip editTooltip = new Tooltip("Edit (only color for system groups)");

                    {
                        pane.setAlignment(Pos.CENTER);
                        editBtn.getStyleClass().add("button-table-edit");
                        deleteBtn.getStyleClass().add("button-table-delete");

                        editBtn.setOnAction(e -> {
                            CategoryGroup group = getTableView().getItems().get(getIndex());
                            startEditGroup(group);
                        });

                        deleteBtn.setOnAction(e -> {
                            CategoryGroup group = getTableView().getItems().get(getIndex());
                            handleDeleteGroup(group);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                            return;
                        }
                        CategoryGroup group = getTableView().getItems().get(getIndex());
                        if (isSystemGroup(group)) {
                            editBtn.setTooltip(editTooltip);
                            pane.getChildren().clear();
                            pane.getChildren().add(editBtn);
                            setGraphic(pane);
                        } else {
                            editBtn.setTooltip(null);
                            pane.getChildren().clear();
                            pane.getChildren().addAll(editBtn, deleteBtn);
                            setGraphic(pane);
                        }
                    }
                };
            }
        });

        groupsTable.setItems(filteredGroups);
    }

    private void setupFilters() {
        filterNameField.textProperty().addListener((obs, old, val) -> applyFilters());
        filterColorPicker.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void applyFilters() {
        String nameFilter = filterNameField.getText().toLowerCase();
        Color colorFilter = filterColorPicker.getValue();
        TransactionType typeFilter = filterTypeCombo.getValue();

        filteredGroups.setPredicate(group -> {
            if (!nameFilter.isEmpty() && !group.getName().toLowerCase().contains(nameFilter))
                return false;
            if (colorFilter != null) {
                String hex = toRgbString(colorFilter);
                if (!group.getColor().equalsIgnoreCase(hex))
                    return false;
            }
            return typeFilter == null || group.getType() == typeFilter;
        });
    }

    private void setupItemList() {
        itemsListView.setItems(items);
        itemsListView.setCellFactory(lv -> new ListCell<CategoryItem>() {
            private final HBox container = new HBox(10);
            private final Label nameLabel = new Label();
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Region spacer = new Region();

            {
                container.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(spacer, Priority.ALWAYS);
                editBtn.getStyleClass().add("button-table-edit");
                deleteBtn.getStyleClass().add("button-table-delete");

                editBtn.setOnAction(e -> {
                    CategoryItem item = getItem();
                    if (item != null) startEditItem(item);
                });

                deleteBtn.setOnAction(e -> {
                    CategoryItem item = getItem();
                    if (item != null) handleDeleteItem(item);
                });

                container.getChildren().addAll(nameLabel, spacer, editBtn, deleteBtn);
            }

            @Override
            protected void updateItem(CategoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                boolean protectedItem = isProtectedItem(item);

                if (protectedItem) {
                    setGraphic(null);
                    setText(item.getName());
                    setTooltip(new Tooltip("This subcategory is linked to a Savings Goal and cannot be edited or deleted here."));
                } else {
                    nameLabel.setText(item.getName());
                    setGraphic(container);
                    setTooltip(null);
                }
            }
        });
    }

    private void loadGroups() {
        User user = userSession.getCurrentUser();
        masterGroups.setAll(categoryService.getGroupsForUser(user).stream()
                .filter(g -> !SYSTEM_GROUP_SYSTEM.equalsIgnoreCase(g.getName()))
                .collect(Collectors.toList()));
    }

    private void loadItemsForGroup(CategoryGroup group) {
        if (group == null) {
            items.clear();
        } else {
            items.setAll(categoryService.getItemsForGroup(group.getId()));
        }
    }

    private void clearGroupForm() {
        selectedGroup = null;
        groupNameField.clear();
        groupTypeCombo.getSelectionModel().selectFirst();
        groupColorPicker.setValue(Color.web("#1ABC9C"));
        saveGroupBtn.setText("Create Group");
        subcategoriesSection.setDisable(true);
        items.clear();
        itemNameField.clear();
        editingItem = null;
        addItemBtn.setText("Add Subcategory");
        groupsTable.getSelectionModel().clearSelection();

        groupNameField.setDisable(false);
        groupTypeCombo.setDisable(false);
        groupNameField.setTooltip(null);
        groupTypeCombo.setTooltip(null);
        addItemBtn.setDisable(false);
        addItemBtn.setTooltip(null);
    }

    private void startEditGroup(CategoryGroup group) {
        if (group == null) return;

        selectedGroup = group;
        groupNameField.setText(group.getName());
        groupTypeCombo.setValue(group.getType());
        groupColorPicker.setValue(Color.web(group.getColor()));

        if (isSystemGroup(group)) {
            groupNameField.setDisable(true);
            groupTypeCombo.setDisable(true);
            groupNameField.setTooltip(new Tooltip("System group name cannot be changed"));
            groupTypeCombo.setTooltip(new Tooltip("System group type cannot be changed"));
            saveGroupBtn.setText("Update Color Only");

            if (group.getName().equalsIgnoreCase(SYSTEM_GROUP_SAVINGS)) {
                subcategoriesSection.setDisable(false);
                addItemBtn.setDisable(true);
                addItemBtn.setTooltip(new Tooltip("Cannot add subcategories to Savings group. Manage via Savings Goals."));
            } else {
                subcategoriesSection.setDisable(false);
                addItemBtn.setDisable(false);
                addItemBtn.setTooltip(null);
            }
        } else {
            groupNameField.setDisable(false);
            groupTypeCombo.setDisable(false);
            groupNameField.setTooltip(null);
            groupTypeCombo.setTooltip(null);
            saveGroupBtn.setText("Update Group");
            subcategoriesSection.setDisable(false);
            addItemBtn.setDisable(false);
            addItemBtn.setTooltip(null);
        }

        loadItemsForGroup(group);
        itemNameField.clear();
        editingItem = null;
        addItemBtn.setText("Add Subcategory");
    }

    @FXML
    private void handleSaveGroup() {
        if (selectedGroup == null) {
            String name = groupNameField.getText().trim();
            if (name.isEmpty()) {
                navigationService.showAlert("Error", "Group name cannot be empty");
                return;
            }
            if (name.equalsIgnoreCase(SYSTEM_GROUP_INCOME) || name.equalsIgnoreCase(SYSTEM_GROUP_SAVINGS) ||
                    name.equalsIgnoreCase(SYSTEM_GROUP_SYSTEM)) {
                navigationService.showAlert("Error", "Group name '" + name + "' is reserved for system use.");
                return;
            }

            String colorHex = toRgbString(groupColorPicker.getValue());
            boolean colorExists = masterGroups.stream()
                    .anyMatch(g -> g.getColor().equalsIgnoreCase(colorHex));
            if (colorExists) {
                navigationService.showAlert("Warning", "This color is already in use. Please select a unique color.");
                return;
            }

            User user = userSession.getCurrentUser();
            TransactionType type = groupTypeCombo.getValue();

            CategoryGroup group = CategoryGroup.builder()
                    .name(name)
                    .type(type)
                    .color(colorHex)
                    .user(user)
                    .build();
            categoryService.saveGroup(group);
            loadGroups();
            startEditGroup(group);
            navigationService.showAlert("Success", "Group saved successfully!");

            if (type == TransactionType.EXPENSE && !name.equalsIgnoreCase("Savings")) {
                BudgetPeriod activePeriod = budgetService.getActivePeriod(user);
                if (activePeriod != null) {
                    boolean exists = budgetService.getBudgetsForPeriod(activePeriod).stream()
                            .anyMatch(b -> b.getCategoryGroup().getId().equals(group.getId()));
                    if (!exists) {
                        budgetService.setCategoryBudget(activePeriod, group, BigDecimal.ZERO);
                    }
                }
            }
        } else {
            if (isSystemGroup(selectedGroup)) {
                selectedGroup.setColor(toRgbString(groupColorPicker.getValue()));
                categoryService.saveGroup(selectedGroup);
                loadGroups();
                startEditGroup(selectedGroup);
                navigationService.showAlert("Success", "Group color updated successfully!");
            } else {
                String name = groupNameField.getText().trim();
                if (name.isEmpty()) {
                    navigationService.showAlert("Error", "Group name cannot be empty");
                    return;
                }

                String colorHex = toRgbString(groupColorPicker.getValue());
                boolean colorExists = masterGroups.stream()
                        .anyMatch(g -> g.getColor().equalsIgnoreCase(colorHex) &&
                                !g.getId().equals(selectedGroup.getId()));
                if (colorExists) {
                    navigationService.showAlert("Warning", "This color is already in use by another group. Please select a unique color.");
                    return;
                }

                selectedGroup.setName(name);
                selectedGroup.setType(groupTypeCombo.getValue());
                selectedGroup.setColor(colorHex);
                categoryService.saveGroup(selectedGroup);
                loadGroups();
                startEditGroup(selectedGroup);
                navigationService.showAlert("Success", "Group updated successfully!");
            }
        }
    }

    @FXML
    private void handleCancelGroup() {
        clearGroupForm();
    }

    private void handleDeleteGroup(CategoryGroup group) {
        if (group == null) return;

        if (isSystemGroup(group)) {
            navigationService.showAlert("Cannot Delete", "System category '" + group.getName() + "' cannot be deleted.");
            return;
        }

        boolean confirm = navigationService.showConfirmation("Delete Group",
                "Delete '" + group.getName() + "'?",
                "All subcategories will also be deleted.");
        if (confirm) {
            categoryService.deleteGroup(group.getId());
            loadGroups();
            clearGroupForm();
        }
    }

    @FXML
    private void handleAddItem() {
        if (selectedGroup == null) {
            navigationService.showAlert("Error", "Select or create a group first");
            return;
        }

        if (SYSTEM_GROUP_SAVINGS.equalsIgnoreCase(selectedGroup.getName())) {
            navigationService.showAlert("Cannot Add",
                    "Cannot add subcategories to the 'Savings' group.\n" +
                            "All envelopes are managed through the Savings Goals section.");
            return;
        }

        String name = itemNameField.getText().trim();
        if (name.isEmpty()) {
            navigationService.showAlert("Error", "Subcategory name cannot be empty");
            return;
        }

        if (SYSTEM_GROUP_INCOME.equalsIgnoreCase(selectedGroup.getName())) {
            User user = userSession.getCurrentUser();
            Set<String> goalNames = savingsService.getUserGoals(user).stream()
                    .map(SavingsGoal::getName)
                    .collect(Collectors.toSet());
            if (goalNames.contains(name)) {
                navigationService.showAlert("Cannot Add",
                        "The name '" + name + "' is already used by a Savings Goal.\n" +
                                "Please choose a different name for your custom income subcategory.");
                return;
            }
        }

        if (editingItem == null) {
            CategoryItem item = CategoryItem.builder()
                    .name(name)
                    .group(selectedGroup)
                    .build();
            categoryService.saveItem(item);
        } else {
            editingItem.setName(name);
            categoryService.saveItem(editingItem);
            editingItem = null;
            addItemBtn.setText("Add Subcategory");
        }

        loadItemsForGroup(selectedGroup);
        itemNameField.clear();
    }

    private void startEditItem(CategoryItem item) {
        if (item == null) return;

        if (isProtectedItem(item)) {
            navigationService.showAlert("Cannot Edit",
                    "This subcategory belongs to a Savings Goal.\n" +
                            "To edit or delete it, please manage the corresponding goal in the Savings section.");
            return;
        }

        editingItem = item;
        itemNameField.setText(item.getName());
        addItemBtn.setText("Update");
    }

    private void handleDeleteItem(CategoryItem item) {
        if (item == null) return;

        if (isProtectedItem(item)) {
            navigationService.showAlert("Cannot Delete",
                    "This subcategory belongs to a Savings Goal.\n" +
                            "To delete it, please delete the corresponding Savings Goal in the Savings section.");
            return;
        }

        boolean confirm = navigationService.showConfirmation("Delete Subcategory",
                "Delete '" + item.getName() + "'?",
                "This cannot be undone.");
        if (confirm) {
            categoryService.deleteItem(item.getId());
            loadItemsForGroup(selectedGroup);
            if (editingItem != null && editingItem.getId().equals(item.getId())) {
                editingItem = null;
                addItemBtn.setText("Add Subcategory");
                itemNameField.clear();
            }
        }
    }

    @FXML
    private void handleResetFilter() {
        filterNameField.clear();
        filterColorPicker.setValue(null);
        filterTypeCombo.setValue(null);
    }

    private String toRgbString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }
}