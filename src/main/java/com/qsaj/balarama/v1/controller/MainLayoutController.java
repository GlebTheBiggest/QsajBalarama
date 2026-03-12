package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.qsaj.balarama.v1.service.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

import java.util.HashMap;
import java.util.Map;

public class MainLayoutController {
    @FXML private BorderPane rootPane;
    @FXML private BorderPane contentPane;
    @FXML private Button btnDashboard;
    @FXML private Button btnTransactions;
    @FXML private Button btnSavings;
    @FXML private Button btnCategories;
    @FXML private Button btnAnalytics;
    @FXML private Button btnProfile;

    private final NavigationService navigationService;
    private final Map<Button, String> viewMapping;
    private Button activeButton;

    @Inject
    public MainLayoutController(NavigationService navigationService) {
        this.navigationService = navigationService;
        this.viewMapping = new HashMap<>();
    }

    @FXML
    public void initialize() {
        initializeViewMapping();
        showDashboard();
    }

    private void initializeViewMapping() {
        viewMapping.put(btnDashboard, "/fxml/dashboard.fxml");
        viewMapping.put(btnTransactions, "/fxml/transactions.fxml");
        viewMapping.put(btnSavings, "/fxml/savings.fxml");
        viewMapping.put(btnCategories, "/fxml/category-settings.fxml");
        viewMapping.put(btnProfile, "/fxml/profile.fxml");
    }

    private void setActive(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("menu-button-active");
        }
        if (button != null) {
            button.getStyleClass().add("menu-button-active");
            activeButton = button;
        }
    }

    private void navigateToView(Button button) {
        String fxmlPath = viewMapping.get(button);
        if (fxmlPath == null) return;
        System.out.println("Loading view: " + fxmlPath); // додати
        try {
            Node view = navigationService.loadView(fxmlPath);
            System.out.println("View loaded, setting to center"); // додати
            contentPane.setCenter(view);
            setActive(button);
        } catch (Exception e) {
            e.printStackTrace(); // додати, щоб побачити стек помилки
            navigationService.showAlert("Navigation Error", "Could not load view: " + fxmlPath);
        }
    }

    @FXML
    private void showDashboard() {
        navigateToView(btnDashboard);
    }

    @FXML
    private void showTransactions() {
        navigateToView(btnTransactions);
    }

    @FXML
    private void showSavingsGoals() {
        navigateToView(btnSavings);
    }

    @FXML
    private void showCategorySettings() {

        navigateToView(btnCategories);
    }

    @FXML
    private void showAnalytics() {
        navigationService.showAlert("Info", "Analytics feature coming soon.");
        setActive(btnAnalytics);
    }

    @FXML
    private void showProfile() {
        navigateToView(btnProfile);
    }

    @FXML
    private void handleLogout() {
        navigationService.navigateTo("/fxml/login.fxml", "Qsaj Balarama - Login", false);
    }
}