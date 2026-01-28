package com.qsaj.balarama.v1.controller;

import com.google.inject.Inject;
import com.google.inject.Injector;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainLayoutController {

    @FXML
    private BorderPane rootPane;

    private final Injector injector;

    @Inject
    public MainLayoutController(Injector injector) {
        this.injector = injector;
    }

    @FXML
    public void initialize() {
        showDashboard();
    }

    // --- МЕТОДИ НАВІГАЦІЇ ---

    @FXML
    private void showDashboard() {
        loadView("/fxml/dashboard.fxml");
    }

    @FXML
    private void showTransactions() {
        loadView("/fxml/transactions.fxml");
    }

    @FXML
    private void showAnalytics() {
        // Поки що заглушка, або можна видалити, якщо замінив на Savings
        System.out.println("Navigate to Analytics");
    }

    @FXML
    private void showSavingsGoals() {
        loadView("/fxml/savings.fxml");
    }

    @FXML
    private void showProfile() {
        loadView("/fxml/profile.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(injector::getInstance);
            Parent loginRoot = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();

            // ВИПРАВЛЕНО: Розмір вікна при виході (1280x800)
            stage.setScene(new Scene(loginRoot, 1280, 800));
            stage.setTitle("Qsaj Balarama - Login");
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- ДОПОМІЖНИЙ МЕТОД ---

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(injector::getInstance);
            Node view = loader.load();
            rootPane.setCenter(view);
        } catch (IOException e) {
            System.err.println("Could not load view: " + fxmlPath);
            e.printStackTrace();
        }
    }
}