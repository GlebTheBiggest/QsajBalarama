package com.qsaj.balarama.v1.service;

import javafx.scene.Node;
import javafx.stage.Stage;

import java.io.IOException;

public interface NavigationService {
    void setPrimaryStage(Stage stage);
    void navigateTo(String fxmlPath, String title, boolean maximized);
    void navigateTo(String fxmlPath, String title, double width, double height);
    Node loadView(String fxmlPath) throws IOException;

    void showAlert(String title, String content);
    boolean showConfirmation(String title, String header, String content); // НОВИЙ МЕТОД
}