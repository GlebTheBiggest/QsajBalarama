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
        // При відкритті програми відразу показуємо Дашборд
        showDashboard();
    }

    // --- МЕТОДИ НАВІГАЦІЇ ---

    @FXML
    private void showDashboard() {
        loadView("/fxml/dashboard.fxml");
    }

    @FXML
    private void showTransactions() {
        // Поки що немає файлу, тому просто вивід в консоль (або створиш transaction.fxml пізніше)
        System.out.println("Navigate to Transactions");
        // loadView("/fxml/transactions.fxml");
    }

    @FXML
    private void showAnalytics() {
        System.out.println("Navigate to Analytics");
    }

    @FXML
    private void showProfile() {
        System.out.println("Navigate to Profile");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Завантажуємо екран логіну
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(injector::getInstance);
            Parent loginRoot = loader.load();

            // Отримуємо поточне вікно і міняємо сцену повністю
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(loginRoot, 1080, 720));
            stage.setTitle("Qsaj Balarama - Login");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- ДОПОМІЖНИЙ МЕТОД ---

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            // Критично важливо: передаємо Guice Injector для створення контролерів вкладених view
            loader.setControllerFactory(injector::getInstance);

            Node view = loader.load();

            // Вставляємо завантажене view у центр BorderPane
            rootPane.setCenter(view);

        } catch (IOException e) {
            System.err.println("Could not load view: " + fxmlPath);
            e.printStackTrace();
        }
    }
}