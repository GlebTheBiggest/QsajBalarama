package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.model.CategoryGroup;
import com.qsaj.balarama.v1.model.CategoryItem;
import com.qsaj.balarama.v1.model.User;
import com.qsaj.balarama.v1.model.enums.TransactionType;
import com.qsaj.balarama.v1.service.NavigationService;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;

public class CategorySettingsController {

    // Left panel fields
    @FXML private TextField groupNameField;
    @FXML private ComboBox<TransactionType> groupTypeCombo;
    @FXML private ColorPicker groupColorPicker;
    @FXML private Button saveGroupBtn;
    @FXML private VBox subcategoriesSection;

    // Subcategory fields
    @FXML private TextField itemNameField;
    @FXML private Button addItemBtn;
    @FXML private ListView<CategoryItem> itemsListView;

    // Filter controls
    @FXML private TextField filterNameField;
    @FXML private ComboBox<TransactionType> filterTypeCombo;
    @FXML private ColorPicker filterColorPicker;

    // Table
    @FXML private TableView<CategoryGroup> groupsTable;
    @FXML private TableColumn<CategoryGroup, String> colName;
    @FXML private TableColumn<CategoryGroup, TransactionType> colType;
    @FXML private TableColumn<CategoryGroup, String> colColor;
    @FXML private TableColumn<CategoryGroup, Void> colActions;

    private final UserSession userSession;
    private final CategoryServiceImpl categoryService;
    private final NavigationService navigationService;

    private CategoryGroup selectedGroup;
    private CategoryItem editingItem;
    private final ObservableList<CategoryGroup> masterGroups = FXCollections.observableArrayList();
    private final FilteredList<CategoryGroup> filteredGroups = new FilteredList<>(masterGroups, p -> true);
    private final ObservableList<CategoryItem> items = FXCollections.observableArrayList();

    @Inject
    public CategorySettingsController(UserSession userSession,
                                      CategoryServiceImpl categoryService,
                                      NavigationService navigationService) {
        this.userSession = userSession;
        this.categoryService = categoryService;
        this.navigationService = navigationService;
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

    private void setupTypeComboBoxes() {
        groupTypeCombo.setItems(FXCollections.observableArrayList(TransactionType.values()));
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
                    if (item == TransactionType.INCOME) {
                        getStyleClass().add("amount-income");
                    } else {
                        getStyleClass().add("amount-expense");
                    }
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

        colActions.setCellFactory(new Callback<TableColumn<CategoryGroup, Void>, TableCell<CategoryGroup, Void>>() {
            @Override
            public TableCell<CategoryGroup, Void> call(TableColumn<CategoryGroup, Void> param) {
                return new TableCell<CategoryGroup, Void>() {
                    private final Button editBtn = new Button("Edit");
                    private final Button deleteBtn = new Button("Delete");
                    private final HBox pane = new HBox(5, editBtn, deleteBtn);

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
                        setGraphic(empty ? null : pane);
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
            if (!nameFilter.isEmpty() && !group.getName().toLowerCase().contains(nameFilter)) {
                return false;
            }
            if (colorFilter != null) {
                String hex = toRgbString(colorFilter);
                if (!group.getColor().equalsIgnoreCase(hex)) {
                    return false;
                }
            }
            return typeFilter == null || group.getType() == typeFilter;
        });
    }

    private void setupItemList() {
        itemsListView.setItems(items);
        itemsListView.setCellFactory(lv -> new ListCell<CategoryItem>() {
            private final HBox container = new HBox(10);
            private final Label nameLabel = new Label();
            // Тепер кнопки виглядають як у таблиці (Edit / Delete)
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Region spacer = new Region();

            {
                container.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                // Використовуємо ті ж самі стилі, що і в таблиці
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
                } else {
                    nameLabel.setText(item.getName());
                    setGraphic(container);
                }
            }
        });
    }

    private void loadGroups() {
        User user = userSession.getCurrentUser();
        masterGroups.setAll(categoryService.getGroupsForUser(user));
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
    }

    private void startEditGroup(CategoryGroup group) {
        if (group == null) return;
        selectedGroup = group;
        groupNameField.setText(group.getName());
        groupTypeCombo.setValue(group.getType());
        groupColorPicker.setValue(Color.web(group.getColor()));
        saveGroupBtn.setText("Update Group");
        subcategoriesSection.setDisable(false);
        loadItemsForGroup(group);
        itemNameField.clear();
        editingItem = null;
        addItemBtn.setText("Add Subcategory");
    }

    private void startEditItem(CategoryItem item) {
        if (item == null) return;
        editingItem = item;
        itemNameField.setText(item.getName());
        addItemBtn.setText("Update");
    }

    @FXML
    private void handleSaveGroup() {
        String name = groupNameField.getText().trim();
        if (name.isEmpty()) {
            navigationService.showAlert("Error", "Group name cannot be empty");
            return;
        }

        String colorHex = toRgbString(groupColorPicker.getValue());

        boolean colorExists = masterGroups.stream()
                .anyMatch(g -> g.getColor().equalsIgnoreCase(colorHex) &&
                        (selectedGroup == null || !g.getId().equals(selectedGroup.getId())));

        if (colorExists) {
            navigationService.showAlert("Warning", "This color is already in use by another group.\nPlease select a unique color to avoid confusion.");
            return;
        }

        User user = userSession.getCurrentUser();
        TransactionType type = groupTypeCombo.getValue();

        if (selectedGroup == null) {
            CategoryGroup group = CategoryGroup.builder()
                    .name(name)
                    .type(type)
                    .color(colorHex)
                    .user(user)
                    .build();
            categoryService.saveGroup(group);
            startEditGroup(group);
        } else {
            selectedGroup.setName(name);
            selectedGroup.setType(type);
            selectedGroup.setColor(colorHex);
            categoryService.saveGroup(selectedGroup);
        }

        loadGroups();
        navigationService.showAlert("Success", "Group saved successfully!");
    }

    @FXML
    private void handleCancelGroup() {
        clearGroupForm();
    }

    private void handleDeleteGroup(CategoryGroup group) {
        if (group == null) return;
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
        String name = itemNameField.getText().trim();
        if (name.isEmpty()) {
            navigationService.showAlert("Error", "Item name cannot be empty");
            return;
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

    private void handleDeleteItem(CategoryItem item) {
        if (item == null) return;
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