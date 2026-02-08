package com.qsaj.balarama.v1.service.impl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.qsaj.balarama.v1.service.NavigationService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.Setter;

import java.io.IOException;

@Singleton
public class NavigationServiceImpl implements NavigationService {
    private final Injector injector;
    @Setter
    private Stage primaryStage;

    @Inject
    public NavigationServiceImpl(Injector injector) {
        this.injector = injector;
    }

    @Override
    public void navigateTo(String fxmlPath, String title, boolean maximized) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(injector::getInstance);
            Parent root = loader.load();

            if (primaryStage == null) {
                throw new IllegalStateException("Primary stage is not set. Call setPrimaryStage() first.");
            }

            primaryStage.setScene(new Scene(root));
            primaryStage.setMaximized(maximized);
            primaryStage.setTitle(title);
        } catch (IOException e) {
            showAlert("Navigation Error", "Could not load view: " + fxmlPath);
        }
    }

    @Override
    public void navigateTo(String fxmlPath, String title, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(injector::getInstance);
            Parent root = loader.load();

            if (primaryStage == null) {
                throw new IllegalStateException("Primary stage is not set. Call setPrimaryStage() first.");
            }

            primaryStage.setScene(new Scene(root, width, height));
            primaryStage.setMaximized(false);
            primaryStage.setTitle(title);
        } catch (IOException e) {
            showAlert("Navigation Error", "Could not load view: " + fxmlPath);
        }
    }

    @Override
    public Node loadView(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(injector::getInstance);
        return loader.load();
    }

    @Override
    public void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}